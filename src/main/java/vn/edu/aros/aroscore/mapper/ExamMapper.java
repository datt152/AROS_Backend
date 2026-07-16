package vn.edu.aros.aroscore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.entity.Exam;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "teacher.email", target = "teacherEmail")
    // Dùng biểu thức Java để lấy tổng số câu hỏi từ size của List
    @Mapping(target = "totalQuestions", expression = "java(exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0)")
    ExamResponse toResponse(Exam exam);
}