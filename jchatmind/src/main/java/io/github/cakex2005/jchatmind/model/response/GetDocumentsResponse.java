package io.github.cakex2005.jchatmind.model.response;

import io.github.cakex2005.jchatmind.model.vo.DocumentVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetDocumentsResponse {
    private DocumentVO[] documents;
}

