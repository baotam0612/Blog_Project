package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.RoleEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.entity.UserRoleEntity;
import com.backlog.backend.model.dto.UserDTO;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.model.request.UserRequest;
import com.backlog.backend.model.response.ChangeStatusRequest;
import com.backlog.backend.repository.RoleRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
// Không phải khởi tạo constructor cho biến final
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    final ModelMapper modelMapper;
    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final UserRoleRepository userRoleRepository;
    PasswordEncoder passwordEncoder;

//    public UserServiceImpl(ModelMapper modelMapper, UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository){
//        this.modelMapper = modelMapper;
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.userRoleRepository = userRoleRepository;
//    }
    private UserDTO toDTO(UserEntity userEntity){
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setEmail(userEntity.getEmail());
        userDTO.setDisplayName(userEntity.getDisplayName());
        return userDTO;
    }

    // Get All User
    @Override
    public List<UserDTO> getAllUser() {
        List<UserEntity> listUser = userRepository.findAll();
        List<UserDTO> listUserDTO = new ArrayList<>();
        for(UserEntity user : listUser){
            UserDTO userDTO = toDTO(user);
            listUserDTO.add(userDTO);
        }
        return listUserDTO;
    }

    @Override
    public UserDTO getCurrentUser(String userName) {
        UserEntity user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        return toDTO(user);
    }

    // Create User
    @Transactional
    @Override
    public UserDTO createUser(UserRequest userRequest) {
        UserEntity user = modelMapper.map(userRequest, UserEntity.class);
        user.setUsername(userRequest.getUsername());
        String userName = userRequest.getUsername();
        String email = userRequest.getEmail();
        if(userRepository.existsByUsername(userName)){
            throw new RuntimeException("Username bị trùng ");
        }
        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Email bị trùng");
        }

        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));

        user.setIsActive(true);
        RoleEntity roleEntity = roleRepository.findByCode("READER")
                .orElseThrow(() -> new RuntimeException("Khong tim thay role"));
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(user);
        userRole.setRole(roleEntity);
//        List<UserRoleEntity> list = new ArrayList<>();
//        list.add(userRole);
//        user.setListUserRole(list);
//        roleEntity.setListUserRole(list);
//        System.out.println(list);

        UserEntity newUser = userRepository.save(user);

  //      System.out.println(list);
        userRoleRepository.save(userRole);
        return toDTO(newUser);
    }

    // Update Status User
    @Override
    @Transactional
    public UserDTO updateStatusUser(
            Long userId,
            ChangeStatusRequest request
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy user"
                ));

        boolean active = request.getStatus() == 1;

        user.setIsActive(active);

        UserEntity savedUser = userRepository.save(user);

        return toDTO(savedUser);
    }

    // Update User Infor
    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy user"));

        if (!Objects.equals(request.getUsername(), user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username bị trùng");
        }
        if (!Objects.equals(request.getEmail(), user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email bị trùng");
        }


        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());

        Set<String> requestedCodes = request.getRoles();
        if (requestedCodes == null) {
            requestedCodes = userRoleRepository.findAllByUser(user).stream()
                    .map(link -> link.getRole().getCode())
                    .collect(Collectors.toSet());
        }
        Map<String, RoleEntity> requestedRoles = new HashMap<>();
        for (String code : requestedCodes) {
            RoleEntity role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + code));
            requestedRoles.put(code, role);
        }

        List<UserRoleEntity> currentLinks = userRoleRepository.findAllByUser(user);
        Set<String> currentCodes = currentLinks.stream()
                .map(link -> link.getRole().getCode())
                .collect(Collectors.toSet());

        // Xóa những role không còn trong request
        for (UserRoleEntity link : currentLinks) {
            if (!requestedCodes.contains(link.getRole().getCode())) {
                userRoleRepository.delete(link);
            }
        }

        // Thêm những role mới
        for (String code : requestedCodes) {
            if (!currentCodes.contains(code)) {
                UserRoleEntity link = new UserRoleEntity();
                link.setUser(user);
                link.setRole(requestedRoles.get(code));
                userRoleRepository.save(link);
            }
        }

        return toDTO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        userRoleRepository.deleteAll(userRoleRepository.findAllByUser(user));
        userRepository.deleteById(id);
    }
}
