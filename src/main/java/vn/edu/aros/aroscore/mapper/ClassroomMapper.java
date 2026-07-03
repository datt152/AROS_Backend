package vn.edu.aros.aroscore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.entity.Classroom;

@Mapper(componentModel = "spring")
public interface ClassroomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "students", ignore = true)
    Classroom toEntity(ClassroomRequest request);

    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "subject.subjectName", target = "subjectName")
    ClassroomResponse toResponse(Classroom classroom);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "students", ignore = true)
    void updateEntityFromRequest(ClassroomRequest request, @MappingTarget Classroom classroom);
}