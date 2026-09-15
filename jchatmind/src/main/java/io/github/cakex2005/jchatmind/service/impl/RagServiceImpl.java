package io.github.cakex2005.jchatmind.service.impl;

import io.github.cakex2005.jchatmind.mapper.ChunkBgeM3Mapper;
import io.github.cakex2005.jchatmind.model.entity.ChunkBgeM3;
import io.github.cakex2005.jchatmind.service.RagService;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.ai.embedding.EmbeddingModel;
import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    // 封装本地的模型调用
    private final EmbeddingModel embeddingModel;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    public RagServiceImpl(EmbeddingModel embeddingModel, ChunkBgeM3Mapper chunkBgeM3Mapper) {
        this.embeddingModel = embeddingModel;
        this.chunkBgeM3Mapper = chunkBgeM3Mapper;
    }

    @Data
    private static class EmbeddingResponse {
        private float[] embedding;
    }

    private float[] doEmbed(String text) {
        return embeddingModel.embed(text);
    }

    @Override
    public float[] embed(String text) {
        return doEmbed(text);
    }

    @Override
    public List<String> similaritySearch(String kbId, String title) {
        String queryEmbedding = toPgVector(doEmbed(title));
        List<ChunkBgeM3> chunks = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, 3);
        return chunks.stream().map(ChunkBgeM3::getContent).toList();
    }

    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
