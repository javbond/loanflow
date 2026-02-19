package com.loanflow.document.mapper;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.dto.response.DocumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Document entity
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "documentType", expression = "java(document.getDocumentType() != null ? document.getDocumentType().name() : null)")
    @Mapping(target = "category", expression = "java(document.getCategory() != null ? document.getCategory().name() : null)")
    @Mapping(target = "status", expression = "java(document.getStatus() != null ? document.getStatus().name() : null)")
    @Mapping(target = "formattedFileSize", expression = "java(document.getFormattedFileSize())")
    @Mapping(target = "expired", expression = "java(document.isExpired())")
    @Mapping(target = "expiringSoon", expression = "java(document.isExpiringSoon())")
    @Mapping(target = "downloadUrl", ignore = true)
    @Mapping(target = "version", source = "version")
    DocumentResponse toResponse(Document document);
}
