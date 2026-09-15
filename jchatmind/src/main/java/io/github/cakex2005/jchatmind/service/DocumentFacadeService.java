package io.github.cakex2005.jchatmind.service;

import io.github.cakex2005.jchatmind.model.request.CreateDocumentRequest;
import io.github.cakex2005.jchatmind.model.request.UpdateDocumentRequest;
import io.github.cakex2005.jchatmind.model.response.CreateDocumentResponse;
import io.github.cakex2005.jchatmind.model.response.GetDocumentsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentFacadeService {
    GetDocumentsResponse getDocuments();

    GetDocumentsResponse getDocumentsByKbId(String kbId);

    CreateDocumentResponse createDocument(CreateDocumentRequest request);

    CreateDocumentResponse uploadDocument(String kbId, MultipartFile file);

    void deleteDocument(String documentId);

    void updateDocument(String documentId, UpdateDocumentRequest request);
}
