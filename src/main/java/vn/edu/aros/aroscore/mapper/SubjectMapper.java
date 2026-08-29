package vn.edu.aros.aroscore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.aros.aroscore.dto.request.SubjectRequest;
import vn.edu.aros.aroscore.dto.response.SubjectResponse;
import vn.edu.aros.aroscore.entity.Subject;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lecturer", ignore = true)
    @Mapping(target = "classrooms", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Subject toEntity(SubjectRequest request);

    @Mapping(source = "lecturer.id", target = "lecturerId")
    SubjectResponse toResponse(Subject subject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lecturer", ignore = true)
    @Mapping(target = "classrooms", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(SubjectRequest request, @MappingTarget Subject subject);
}
