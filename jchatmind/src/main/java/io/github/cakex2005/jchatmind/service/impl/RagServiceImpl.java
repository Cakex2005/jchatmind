package io.github.cakex2005.jchatmind.service.impl;

import io.github.cakex2005.jchatmind.mapper.ChunkBgeM3Mapper;
import io.github.cakex2005.jchatmind.model.entity.ChunkBgeM3;
import io.github.cakex2005.jchatmind.service.RagService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RagServiceImpl implements RagService {

    /** 单次相似度检索返回的 top-N。 */
    private static final int DEFAULT_TOP_K = 10;

    /** 混合检索时各检索源各自取回的候选数（进入 RRF 融合）。 */
    private static final int HYBRID_CANDIDATE_TOP_K = 30;

    /** RRF 融合公式中的 k 参数，标准值 60。 */
    private static final double RRF_K = 60.0;

    /** BM25 词频饱和参数。 */
    private static final double BM25_K1 = 1.5;

    /** BM25 文档长度归一化参数。 */
    private static final double BM25_B = 0.75;

    /** 标题在 BM25 中的权重倍率，标题优先。 */
    private static final double TITLE_WEIGHT = 2.0;

    /** 正文在 BM25 中的权重倍率。 */
    private static final double CONTENT_WEIGHT = 1.0;

    /** 标题命中时 RRF 得分的额外加成。 */
    private static final double TITLE_BOOST = 0.01;

    private final EmbeddingModel embeddingModel;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    public RagServiceImpl(EmbeddingModel embeddingModel, ChunkBgeM3Mapper chunkBgeM3Mapper) {
        this.embeddingModel = embeddingModel;
        this.chunkBgeM3Mapper = chunkBgeM3Mapper;
    }

    // ------------------------------------------------------------------
    // 原有 RAG 方法：保留，便于与混合检索做对比
    // ------------------------------------------------------------------

    private float[] doEmbed(String query) {
        return embeddingModel.embed(query);
    }

    @Override
    public List<float[]> embedBatch(List<String> batch) {
        return embeddingModel.embed(batch);
    }

    /** 纯向量检索：query 嵌入后与 chunk_bge_m3.embedding 做余弦距离（<->）排序。 */
    @Override
    public List<String> similaritySearch(String kbId, String query) {
        String queryEmbedding = toPgVector(doEmbed(query));
        List<ChunkBgeM3> queryChunks = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, DEFAULT_TOP_K);
        return queryChunks.stream().map(ChunkBgeM3::getContent).toList();
    }

    /** 纯向量检索（批量）：与入库 embedBatch 相同批量逻辑。 */
    @Override
    public List<List<String>> similaritySearchBatch(String kbId, List<String> queries) {
        List<float[]> embeddings = embedBatch(queries);
        if (embeddings.size() != queries.size()) {
            throw new IllegalStateException("批量向量化返回数量不一致: expected=" + queries.size() + ", actual=" + embeddings.size());
        }

        List<List<String>> results = new ArrayList<>(queries.size());
        for (float[] embedding : embeddings) {
            List<ChunkBgeM3> chunks = chunkBgeM3Mapper.similaritySearch(kbId, toPgVector(embedding), DEFAULT_TOP_K);
            results.add(chunks.stream().map(ChunkBgeM3::getContent).toList());
        }
        return results;
    }

    // ------------------------------------------------------------------
    // 混合检索：BM25 关键词 + 向量检索 + RRF 重排（标题优先，正文补充）
    // ------------------------------------------------------------------

    /** 混合检索（单条 query）：BM25 标题优先 + 向量语义，RRF 融合。 */
    @Override
    public List<String> hybridSearch(String kbId, String query) {
        return hybridSearchWithEmbedding(kbId, query, doEmbed(query));
    }

    /** 混合检索（批量 query）：批量向量化 + BM25 本地打分，RRF 融合。 */
    @Override
    public List<List<String>> hybridSearchBatch(String kbId, List<String> queries) {
        // 1) 批量向量化（一次 HTTP 请求多个 query，避免 RPM 限制）
        List<float[]> embeddings = embedBatch(queries);

        // 2) 每个 query 分别走 BM25 + 向量 + RRF
        List<List<String>> results = new ArrayList<>(queries.size());
        for (int i = 0; i < queries.size(); i++) {
            results.add(hybridSearchWithEmbedding(kbId, queries.get(i), embeddings.get(i)));
        }
        return results;
    }

    private List<String> hybridSearchWithEmbedding(String kbId, String query, float[] qEmbedding) {
        // ① 向量检索：取 HYBRID_CANDIDATE_TOP_K 候选
        String queryEmbedding = toPgVector(qEmbedding);
        List<ChunkBgeM3> vectorResults = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, HYBRID_CANDIDATE_TOP_K);

        // ② BM25 关键词检索：拉取该 kbId 全部 chunk，在内存中打分
        List<ChunkBgeM3> allChunks = chunkBgeM3Mapper.selectByKbId(kbId);
        List<ScoredChunk> bm25Results = bm25Search(query, allChunks, HYBRID_CANDIDATE_TOP_K);

        // ③ RRF 融合
        return rrfFuse(query, allChunks, vectorResults, bm25Results);
    }

    // ------------------------------------------------------------------
    // BM25 实现
    // ------------------------------------------------------------------

    private List<ScoredChunk> bm25Search(String query, List<ChunkBgeM3> chunks, int topK) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        int N = chunks.size();
        if (N == 0) {
            return List.of();
        }

        // 统计包含每个 token 的文档数 df(token)
        Map<String, Integer> df = new HashMap<>();
        for (ChunkBgeM3 chunk : chunks) {
            Set<String> uniqueTokens = new HashSet<>(tokenize(extractTitle(chunk)));
            uniqueTokens.addAll(tokenize(chunk.getContent()));
            for (String token : uniqueTokens) {
                df.merge(token, 1, Integer::sum);
            }
        }

        // 平均长度（标题+正文合并后的 token 长度）
        double avgLen = 0;
        for (ChunkBgeM3 chunk : chunks) {
            avgLen += tokenize(extractTitle(chunk)).size() + tokenize(chunk.getContent()).size();
        }
        avgLen = avgLen / (double) N;

        // 对每个文档计算 BM25
        List<ScoredChunk> scored = new ArrayList<>();
        for (ChunkBgeM3 chunk : chunks) {
            double score = 0;

            // 标题部分（权重更高）
            List<String> titleTokens = tokenize(extractTitle(chunk));
            score += TITLE_WEIGHT * bm25ScoreForOneField(queryTokens, titleTokens, df, N, avgLen);

            // 正文部分（补充）
            List<String> contentTokens = tokenize(chunk.getContent());
            score += CONTENT_WEIGHT * bm25ScoreForOneField(queryTokens, contentTokens, df, N, avgLen);

            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.size() > topK ? new ArrayList<>(scored.subList(0, topK)) : scored;
    }

    private double bm25ScoreForOneField(List<String> queryTokens, List<String> fieldTokens,
                                        Map<String, Integer> df, int N, double avgLen) {
        if (fieldTokens.isEmpty()) {
            return 0.0;
        }
        Map<String, Integer> tf = new HashMap<>();
        for (String t : fieldTokens) {
            tf.merge(t, 1, Integer::sum);
        }
        double len = fieldTokens.size();

        double score = 0.0;
        for (String term : queryTokens) {
            int termTf = tf.getOrDefault(term, 0);
            if (termTf == 0) {
                continue;
            }
            int docFreq = df.getOrDefault(term, 0);
            double idf = Math.log((N - docFreq + 0.5) / (docFreq + 0.5) + 1.0);
            double denominator = termTf + BM25_K1 * (1 - BM25_B + BM25_B * len / avgLen);
            score += idf * (termTf * (BM25_K1 + 1)) / denominator;
        }
        return score;
    }

    // ------------------------------------------------------------------
    // RRF 融合
    // ------------------------------------------------------------------

    private List<String> rrfFuse(String query, List<ChunkBgeM3> allChunks,
                                 List<ChunkBgeM3> vectorResults, List<ScoredChunk> bm25Results) {
        // id -> chunk（顺序保留 chunk 原始写入顺序）
        Map<String, ChunkBgeM3> byId = new LinkedHashMap<>();
        for (ChunkBgeM3 c : allChunks) {
            byId.put(c.getId(), c);
        }

        // RRF 得分表
        Map<String, Double> scores = new HashMap<>();
        Set<String> appeared = new HashSet<>();

        // 向量排名：第 1 名 rank=1
        for (int i = 0; i < vectorResults.size(); i++) {
            String id = vectorResults.get(i).getId();
            scores.merge(id, 1.0 / (RRF_K + (i + 1)), Double::sum);
            appeared.add(id);
        }

        // BM25 排名
        for (int i = 0; i < bm25Results.size(); i++) {
            String id = bm25Results.get(i).chunk.getId();
            scores.merge(id, 1.0 / (RRF_K + (i + 1)), Double::sum);
            appeared.add(id);
        }

        // 标题命中额外加分：query tokens 与 chunk 标题有重叠 → 小 boost，标题优先
        Set<String> queryTokens = new HashSet<>(tokenize(query));
        for (String id : appeared) {
            ChunkBgeM3 chunk = byId.get(id);
            if (chunk != null) {
                Set<String> titleTokens = new HashSet<>(tokenize(extractTitle(chunk)));
                titleTokens.retainAll(queryTokens);
                if (!titleTokens.isEmpty()) {
                    scores.merge(id, TITLE_BOOST, Double::sum);
                }
            }
        }

        // 按 RRF 得分降序，取 top-K
        List<String> orderedIds = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(DEFAULT_TOP_K)
                .toList();

        List<String> result = new ArrayList<>(orderedIds.size());
        for (String id : orderedIds) {
            ChunkBgeM3 c = byId.get(id);
            if (c != null && c.getContent() != null) {
                result.add(c.getContent());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 分词与工具方法
    // ------------------------------------------------------------------

    /**
     * 轻量分词：
     * - 中文/连续汉字：按 bigram（连续两字）切分，适配没有外部中文分词依赖的场景
     * - 英文/数字：按空白和标点拆分为单词
     * 示例："怎么申请退款" → [怎么, 么申, 申请, 请退, 退款]
     */
    static List<String> tokenize(String text) {
        if (text == null) {
            return List.of();
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder cjk = new StringBuilder();
        StringBuilder others = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                if (others.length() > 0) {
                    tokens.add(others.toString().toLowerCase());
                    others.setLength(0);
                }
                cjk.append(c);
            } else {
                if (cjk.length() > 0) {
                    // bigram 切分连续汉字
                    for (int j = 0; j < cjk.length() - 1; j++) {
                        tokens.add(cjk.substring(j, j + 2));
                    }
                    cjk.setLength(0);
                }
                if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                    others.append(c);
                } else if (others.length() > 0) {
                    tokens.add(others.toString().toLowerCase());
                    others.setLength(0);
                }
            }
        }
        if (cjk.length() > 0) {
            for (int j = 0; j < cjk.length() - 1; j++) {
                tokens.add(cjk.substring(j, j + 2));
            }
        }
        if (others.length() > 0) {
            tokens.add(others.toString().toLowerCase());
        }
        return tokens;
    }

    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 从 chunk 的 metadata（JSON: {"title":"..."}）提取标题。
     * 如果 metadata 为空或没有 title 字段，返回空字符串。
     */
    private String extractTitle(ChunkBgeM3 chunk) {
        if (chunk.getMetadata() == null || chunk.getMetadata().isBlank()) {
            return "";
        }
        String md = chunk.getMetadata().trim();
        // 格式：{"title":"xxx"}
        int idx = md.indexOf("\"title\"");
        if (idx < 0) {
            return "";
        }
        // 找到冒号后的第一个双引号（值开始）
        int colonIdx = md.indexOf(':', idx);
        if (colonIdx < 0) {
            return "";
        }
        int quoteStart = colonIdx + 1;
        while (quoteStart < md.length() && md.charAt(quoteStart) != '\"') {
            quoteStart++;
        }
        if (quoteStart >= md.length()) {
            return "";
        }
        int contentStart = quoteStart + 1;
        int contentEnd = contentStart;
        while (contentEnd < md.length()) {
            char c = md.charAt(contentEnd);
            if (c == '\\') {
                contentEnd += 2; // 跳过转义字符
            } else if (c == '\"') {
                break;
            } else {
                contentEnd++;
            }
        }
        if (contentEnd >= md.length()) {
            return md.substring(contentStart).trim();
        }
        return md.substring(contentStart, contentEnd).trim();
    }

    /** BM25 打分后的排名结果。 */
    static class ScoredChunk {
        final ChunkBgeM3 chunk;
        final double score;
        ScoredChunk(ChunkBgeM3 chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
