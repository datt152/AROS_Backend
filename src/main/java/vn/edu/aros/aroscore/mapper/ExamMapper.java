package vn.edu.aros.aroscore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.entity.Exam;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "subject.subjectName", target = "subjectName")
    @Mapping(source = "teacher.email", target = "teacherEmail")
    @Mapping(target = "totalQuestions", expression = "java(exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0)")
    @Mapping(target = "classroomIds", ignore = true)
    @Mapping(target = "config", ignore = true)
    ExamResponse toResponse(Exam exam);
}
