package com.skripsi.koma.service.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.skripsi.koma.dto.ApiResponse;
import com.skripsi.koma.dto.authentication.LoginDTO;
import com.skripsi.koma.dto.authentication.RegisterDTO;
import com.skripsi.koma.dto.authentication.ResetPasswordDTO;
import com.skripsi.koma.dto.user.UserDetailDTO;
import com.skripsi.koma.enums.Role;
import com.skripsi.koma.enums.StatusCode;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.model.user.UserRoleModel;
import com.skripsi.koma.repository.user.UserRepository;
import com.skripsi.koma.repository.user.UserRoleRepository;
import com.skripsi.koma.service.email.EmailService;
import com.skripsi.koma.util.CustomExceptions;
import com.skripsi.koma.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserRoleRepository roleUserRepository;
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final JwtUtil jwtUtil;

  public ApiResponse<UserDetailDTO> getUserDetailById(Long id) {
    UserModel user = userRepository.findById(id)
        .orElseThrow(
            () -> new CustomExceptions(HttpStatus.NOT_FOUND, "User dengan ID " + id + " tidak ditemukan", null));

    UserDetailDTO userDetailDTO = UserDetailDTO.mapToDTO(user);
    return new ApiResponse<UserDetailDTO>(true, "User detail berhasil diambil", userDetailDTO);
  }

  public ApiResponse<LoginDTO> login(String email, String password) throws UsernameNotFoundException {

    UserModel user = userRepository.findByEmail(email).orElse(null);

    if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "User Not Found", null);
    }

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

    final Authentication authentication = authenticationManager.authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String token = jwtUtil.generateToken(user.getEmail());
    user.setJwtToken(token);
    userRepository.save(user);

    LoginDTO loginResponse = new LoginDTO();
    loginResponse.setJwtToken(token);
    loginResponse.setUserDetail(UserDetailDTO.mapToDTO(user));
    return new ApiResponse<LoginDTO>(true, "Login successful", loginResponse);

  }

  public ApiResponse<RegisterDTO> registerUser(RegisterDTO request) {

    log.info("Registering user with email: {}", request.getEmail());
    // FORMAT EMAIL - Check email format
    String email = request.getEmail();
    String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
    Pattern emailPattern = Pattern.compile(emailRegex);
    Matcher emailMatcher = emailPattern.matcher(email);
    if (!emailMatcher.matches()) {
      log.error("Invalid email format for email: {}", email);
      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Format Email Invalid", null);

    }

    // Email & phonenumber terdaftar
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.error("Email already registered: {}", request.getEmail());
      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Email Sudah Terdaftar", null);
    }

    // ROLEID
    Optional<UserRoleModel> roleUser = roleUserRepository.findById(request.getRoleId());
    if (roleUser.isEmpty()) {
      log.error("Invalid Role ID: {}", request.getRoleId());

      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Role ID Invalid", null);
    }

    // Validasi password
    String password = request.getPassword();
    String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(password);
    if (!matcher.matches()) {
      log.error("Invalid password format for password: {}", password);
      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Password Invalid", null);

    }

    // Buat user baru
    UserModel user = new UserModel();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setDateOfBirth(request.getDateOfBirth());
    // user.setJob(request.getJob());
    user.setGender(request.getGender());
    user.setRoleId(roleUser.get());
    user.setAccumulatedPoint(0);

    // Save user to the database
    userRepository.save(user);
    request.setId(user.getId());

    log.info("User registered successfully: {}", request.getEmail());
    // Auto login after successful registration
    ApiResponse<LoginDTO> loginResponse = login(request.getEmail(), request.getPassword());
    request.setLoginDetail(loginResponse.getData());
    return new ApiResponse<RegisterDTO>(true, "User registered and logged in successfully.", request);
  }

  public ApiResponse<List<UserDetailDTO>> getAllUsers() {
    List<UserModel> users = userRepository.findAll();
    List<UserDetailDTO> userDTOs = new ArrayList<>();
    for (UserModel user : users) {
      userDTOs.add(UserDetailDTO.mapToDTO(user));
    }
    return new ApiResponse<List<UserDetailDTO>>(true, "Daftar user berhasil diambil", userDTOs);
  }

  public ApiResponse<UserDetailDTO> updateUser(Long id, UserDetailDTO request) {
    UserModel user = userRepository.findById(id)
        .orElseThrow(
            () -> new CustomExceptions(HttpStatus.NOT_FOUND, "User dengan ID " + id + " tidak ditemukan", null));

    if (request.getName() != null) {
      user.setName(request.getName());
    }
    if (request.getEmail() != null) {
      user.setEmail(request.getEmail());
    }
    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }
    if (request.getPassword() != null) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    if (request.getDateOfBirth() != null) {
      user.setDateOfBirth(request.getDateOfBirth());
    }
    if (request.getJob() != null) {
      user.setJob(request.getJob());
    }
    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }
    if (request.getRoleId() != null) {
      UserRoleModel roleUser = roleUserRepository.findById(request.getRoleId())
          .orElseThrow(() -> new CustomExceptions(HttpStatus.BAD_REQUEST, "Role ID Invalid", null));
      user.setRoleId(roleUser);
    }

    user.setProfilePicture(request.getProfilePicture());

    userRepository.save(user);
    return new ApiResponse<UserDetailDTO>(true, "User berhasil diperbarui", UserDetailDTO.mapToDTO(user));
  }

  public ApiResponse<Void> deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new CustomExceptions(HttpStatus.NOT_FOUND, "User dengan ID " + id + " tidak ditemukan", null);
    }
    userRepository.deleteById(id);
    return new ApiResponse<Void>(true, "User berhasil dihapus", null);
  }

  public String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new CustomExceptions(StatusCode.FORBIDDEN);
    }
    return authentication.getName();
  }

  public UserModel getUser() {
    return userRepository.findByEmail(getUsername()).get();
  }

  public void validateRole(UserModel user, Role allowedRole) {
    if (user == null) {
      throw new CustomExceptions(StatusCode.FORBIDDEN);
    }
    if (user.getRoleId() == null) {
      throw new CustomExceptions(StatusCode.FORBIDDEN);
    }
    if (!user.getRoleId().getRoleName().equals(allowedRole)) {
      throw new CustomExceptions(StatusCode.FORBIDDEN);
    }
  }

  public UserModel getCurrentUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()) {
      return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
    return null;
  }

  public UserModel getUserById(Long id) {
    return userRepository.findById(id).orElseThrow(
        () -> new CustomExceptions(HttpStatus.NOT_FOUND, "User dengan ID " + id + " tidak ditemukan", null));
  }

  public ApiResponse requestResetPassword(String email) {
      UserModel user = userRepository.findByEmail(email)
          .orElseThrow(() -> new RuntimeException("Email tidak ditemukan"));

      String token = UUID.randomUUID().toString();
      LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

      user.setResetToken(token);
      user.setTokenExpiry(expiry);
      userRepository.save(user);
      String resetLink = "http://localhost:4200/reset-password?token=" + token;

      String to = user.getEmail();
      String subject = "Reset Password Akun KOMA";
      String content = "Halo " + user.getName() + ",\n\n" +
              "Silakan klik link berikut untuk mengatur ulang password akun kamu:\n" +
              resetLink + "\n\n" +
              "Link ini berlaku selama 30 menit.\n\n" +
              "Terima kasih.";
      emailService.sendEmail(to, subject, content);
      return new ApiResponse(true, "Link reset password telah dikirim ke email", null);
  }

  public ApiResponse resetPassword(String token, ResetPasswordDTO request) {
      if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new RuntimeException("Konfirmasi password tidak cocok");
    }

    UserModel user = userRepository.findByResetToken(token)
        .orElseThrow(() -> new RuntimeException("Token tidak valid"));

    if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Token sudah kadaluarsa");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    user.setResetToken(null);
    user.setTokenExpiry(null);
    userRepository.save(user);

    return new ApiResponse(true, "Password berhasil diubah", null);
  }

  public ApiResponse<Void> logout() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      SecurityContextHolder.clearContext();
    }
    return new ApiResponse<Void>(true, "Logout successful", null);
  }
}
