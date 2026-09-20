package io.github.cakex2005.jchatmind.service;

import java.util.List;

public interface RagService {
    List<float[]> embedBatch(List<String> batch);

    List<String> similaritySearch(String kbId, String query);

    /**
     * 批量相似度检索：将多个 query 批量向量化（与入库 embedBatch 相同的批量逻辑），
     * 再对每个 query 分别执行向量检索。
     *
     * @param kbId    知识库 ID
     * @param queries 一批 query，顺序与返回结果一一对应
     * @return 每个 query 的 top-N 内容列表
     */
    List<List<String>> similaritySearchBatch(String kbId, List<String> queries);

    /**
     * 混合检索（单条 query）：BM25 关键词检索（标题优先、正文补充） + 向量语义检索，
     * 再通过 RRF（Reciprocal Rank Fusion）重排出最终 Top-N。
     *
     * @param kbId  知识库 ID
     * @param query 用户问题
     * @return 最终重排后的 top-N 内容列表
     */
    List<String> hybridSearch(String kbId, String query);

    /**
     * 混合检索（批量 query）：批量向量化 + BM25 本地打分，再对每个 query 做 RRF 融合。
     *
     * @param kbId    知识库 ID
     * @param queries 一批 query，顺序与返回结果一一对应
     * @return 每个 query 的最终 top-N 内容列表
     */
    List<List<String>> hybridSearchBatch(String kbId, List<String> queries);
}
