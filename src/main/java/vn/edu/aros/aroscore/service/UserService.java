package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.UpdateProfileRequest;
import vn.edu.aros.aroscore.dto.request.UpdateStudentCodeRequest;
import vn.edu.aros.aroscore.dto.response.UserResponse;

public interface UserService {
    UserResponse getMyProfile();

    UserResponse updateMyProfile(UpdateProfileRequest request);

    UserResponse updateMyStudentCode(UpdateStudentCodeRequest request);
}
