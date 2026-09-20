package io.github.cakex2005.jchatmind.service;

import java.util.List;

public interface RagService {
    List<float[]> embedBatch(List<String> batch);

    List<String> similaritySearch(String kbId, String query);
}
