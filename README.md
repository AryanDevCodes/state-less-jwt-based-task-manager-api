# Stateless Authentication Configuration Guide

## Overview
This document outlines the transition from a **stateful** authentication system (session-based) to a **stateless** authentication system (JWT-based) in the Task Manager microservice using Spring Security 7.x.

---

## What is Changed: Stateful vs Stateless

### Stateful Authentication (Previous)
- User logs in → Server creates session → Stores session in memory/database
- Client stores session ID in cookie
- Every request validates against stored session
- Server maintains user state
- **Problem**: Not scalable for microservices, requires session sharing across instances

### Stateless Authentication (Current)
- User logs in → Server generates JWT token → Token returned to client
- Client stores token and sends it with every request
- Server validates token signature without storing state
- All info encoded in JWT token itself
- **Benefit**: Scalable, no session management needed, works across multiple servers

---

## Step-by-Step Configuration Changes

### Step 1: Add Dependencies
**File**: `pom.xml`

Added JWT and Security dependencies:
```xml
<!-- JWT Library -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Step 2: Configure JWT Properties
**File**: `application.properties`

Added JWT configuration:
```properties
jwt.secret=your_base64_encoded_secret_key_minimum_256_bits
jwt.expiration=3600000
spring.jpa.show-sql=false
```

**Important**: The `jwt.secret` must be a Base64-encoded string of at least 256 bits (32 bytes).

### Step 3: Create Security Configuration
**File**: `src/main/java/com/microservice/taskmanager/config/SecurityConfig.java`

**Key Changes**:
- ✅ Added `@EnableMethodSecurity` - Enables `@PreAuthorize` annotations on methods
- ✅ Set `SessionCreationPolicy.STATELESS` - No session management
- ✅ Disabled CSRF - Not needed for stateless APIs
- ✅ Added JWT filter before `UsernamePasswordAuthenticationFilter`
- ✅ Created `PasswordEncoder` bean using `BCryptPasswordEncoder`
- ✅ Created `AuthenticationManager` bean for login authentication

**Configuration Details**:
```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter authenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(authenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) 
            throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .build();
    }
}
```

**What Changed**:
- **Before**: Session-based → Spring created sessions automatically
- **After**: Stateless → No sessions, each request contains token in `Authorization` header

### Step 4: Create JWT Service
**File**: `src/main/java/com/microservice/taskmanager/auth/JwtService.java`

**Responsibilities**:
- ✅ Generate JWT tokens from `UserDetails`
- ✅ Extract username from JWT token
- ✅ Validate token expiration and signature
- ✅ Manage secret key encoding/decoding

**Key Methods**:
```java
public String generateToken(UserDetails userDetails) {
    // Creates token with 1-hour expiration
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSecretKey(), SignatureAlgorithm.HS256)
        .compact();
}

public boolean isTokenValid(String jwt, UserDetails userDetails) {
    final String username = extractUserName(jwt);
    return username.equals(userDetails.getUsername()) 
        && !isTokenExpired(jwt);
}
```

**Configuration**:
```properties
@Value("${jwt.secret}")  # Must include ${} for property placeholder resolution
@Value("${jwt.expiration}")
```

**⚠️ Critical Fix**: Changed `@Value("jwt.secret")` to `@Value("${jwt.secret}")` - without `${}`, Spring won't resolve the property value.

### Step 5: Create JWT Authentication Filter
**File**: `src/main/java/com/microservice/taskmanager/auth/JwtAuthenticationFilter.java`

**Responsibilities**:
- ✅ Intercepts every HTTP request
- ✅ Extracts JWT from `Authorization: Bearer <token>` header
- ✅ Validates token using `JwtService`
- ✅ Sets authentication in `SecurityContext`
- ✅ Skips validation for public endpoints (`/api/auth/**`)

**Filter Logic**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain) throws ServletException, IOException {
    
    // Skip filter for public endpoints
    if (shouldNotFilter(request)) {
        filterChain.doFilter(request, response);
        return;
    }

    final String authHeader = request.getHeader("Authorization");
    
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    final String jwt = authHeader.substring(7);
    final String userEmail = jwtService.extractUserName(jwt);

    if (userEmail != null) {
        UserDetails userDetails = userDetailsService
            .loadUserByUsername(userEmail);
        
        if (jwtService.isTokenValid(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext()
                .setAuthentication(authToken);
        }
    }
    filterChain.doFilter(request, response);
}

@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return pathMatcher.match("/api/auth/**", 
        request.getServletPath());
}
```

**What Changed**:
- **Before**: Automatic session creation and validation
- **After**: Manual token extraction and validation per request

### Step 6: Create Authentication Service
**File**: `src/main/java/com/microservice/taskmanager/service/AuthService.java`

**Responsibilities**:
- ✅ Handle user registration with password hashing
- ✅ Handle user login with authentication
- ✅ Generate JWT token after successful authentication

**Methods**:
```java
public void registerUser(RegisterRequestDTO dto) {
    User user = new User();
    user.setEmail(dto.getEmail());
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setRole(Role.USER);
    userRepository.save(user);
}

public LoginResponseDTO login(LoginDTO dto) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            dto.getEmail(), dto.getPassword()));
    
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow();
    String token = jwtService.generateToken(user);
    
    return LoginResponseDTO.builder()
        .token(token)
        .build();
}
```

**What Changed**:
- **Before**: Session creation after login
- **After**: JWT token generation after login

### Step 7: Create User Details Service
**File**: `src/main/java/com/microservice/taskmanager/auth/CustomerUserDetailsService.java`

**Responsibilities**:
- ✅ Load user from database by email (Spring Security interface)
- ✅ Used by both authentication and JWT validation

```java
@Override
public UserDetails loadUserByUsername(String email) 
        throws UsernameNotFoundException {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(
            "User not found with email: " + email));
}
```

### Step 8: Add Method Security with @PreAuthorize
**File**: `src/main/java/com/microservice/taskmanager/controller/TaskController.java`

**Requirements**:
- ✅ `@EnableMethodSecurity` annotation on `SecurityConfig`
- ✅ `@PreAuthorize` annotations on controller/service methods

**Usage**:
```java
@GetMapping
@PreAuthorize("hasRole('USER')")
public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
    return ResponseEntity.ok(taskService.getMyTask());
}

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @taskService.isOwner(#id, authentication.name)")
public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
}
```

**What Changed**:
- **Before**: Global role-based authorization
- **After**: Method-level role and SpEL-based authorization

### Step 9: Add @Transactional for Database Operations
**File**: `src/main/java/com/microservice/taskmanager/service/TaskService.java`

**Requirement**: JPA delete operations need transaction context

```java
@Transactional
public void deleteTask(long id) {
    taskRepository.deleteByTaskId(id);
}
```

**What Changed**:
- **Before**: Not needed in session context
- **After**: Explicitly required for stateless operations

### Step 10: Use DTOs Instead of Entity Objects
**Changes**:
- ✅ POST `/api/tasks` returns `TaskResponseDto` (not `Task` entity)
- ✅ Prevents circular reference serialization
- ✅ Cleaner API contracts

**Before**:
```java
public ResponseEntity<Task> createTask(...) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(taskService.createTask(request));
}
```

**After**:
```java
public ResponseEntity<TaskResponseDto> createTask(...) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(taskService.createTask(request));
}
```

---

## Complete Request/Response Flow (Stateless)

### 1. Registration Flow
```
POST /api/auth/register
Body: {"email": "user@example.com", "password": "Test@123"}
↓
AuthService.registerUser()
  ├── Encode password with BCryptPasswordEncoder
  ├── Create User with ROLE_USER
  └── Save to database
↓
Response: 200 OK (User created)
```

### 2. Login Flow
```
POST /api/auth/login
Body: {"email": "user@example.com", "password": "Test@123"}
↓
AuthService.login()
  ├── AuthenticationManager.authenticate() - validates credentials
  ├── Load User from database
  ├── JwtService.generateToken(user)
  │   ├── Encode username in JWT
  │   ├── Set expiration to current_time + 1_hour
  │   └── Sign with HMAC-SHA256
  └── Return token
↓
Response: 200 OK {"token": "eyJhbGc..."}
```

### 3. Authenticated Request Flow
```
GET /api/tasks
Header: Authorization: Bearer eyJhbGc...
↓
JwtAuthenticationFilter.doFilterInternal()
  ├── Extract token from Authorization header
  ├── JwtService.extractUserName(token) - decode JWT
  ├── Load UserDetails from database
  ├── JwtService.isTokenValid(token, userDetails)
  │   ├── Check username matches
  │   ├── Check expiration time
  │   └── Verify signature
  ├── Create UsernamePasswordAuthenticationToken
  └── Set in SecurityContext
↓
@PreAuthorize("hasRole('USER')")
  └── Check authorities from SecurityContext
↓
TaskService.getMyTask()
  ├── Get email from SecurityContext.getAuthentication().getName()
  └── Query database
↓
Response: 200 OK [tasks...]
```

---

## API Endpoints Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | ❌ Public | Register new user |
| POST | `/api/auth/login` | ❌ Public | Login and get JWT token |
| GET | `/api/tasks` | ✅ USER | Get user's tasks |
| POST | `/api/tasks` | ✅ USER | Create new task |
| DELETE | `/api/tasks/{id}` | ✅ ADMIN or Owner | Delete task |

---

## Key Components Added

### Annotations
- `@EnableMethodSecurity` - Enables method-level security
- `@PreAuthorize` - Method-level authorization
- `@Transactional` - Transaction management
- `@JsonIgnore` - Prevent circular serialization

### Services
- `JwtService` - Token generation/validation
- `AuthService` - Registration/login
- `CustomerUserDetailsService` - User loading

### Filters
- `JwtAuthenticationFilter` - JWT token extraction and validation

### Beans
- `PasswordEncoder` - BCryptPasswordEncoder
- `AuthenticationManager` - Authentication processing
- `SecurityFilterChain` - HTTP security configuration

---

## Security Features Implemented

✅ **Password Hashing**: BCrypt with auto-generated salt
✅ **JWT Tokens**: HS256 signed, 1-hour expiration
✅ **Role-Based Access**: ROLE_USER and ROLE_ADMIN
✅ **Method-Level Security**: @PreAuthorize expressions
✅ **CSRF Protection**: Disabled (stateless API)
✅ **Session Management**: Disabled (STATELESS)
✅ **Public Endpoints**: /api/auth/** bypasses JWT filter
✅ **Ownership Validation**: Users can only delete their own tasks

---

## Notes

### 🔐 Security Considerations

1. **Secret Key Management**
   - Must be at least 256 bits (32 bytes) when Base64 encoded
   - Should be stored in environment variables, not hardcoded
   - Never expose in version control

2. **Token Expiration**
   - Current: 1 hour (3600000 ms)
   - Consider shorter for sensitive operations
   - Consider refresh tokens for longer sessions

3. **Password Requirements**
   - Minimum 8 characters recommended
   - Should include special characters for production
   - Consider implementing password validation rules

4. **HTTPS Requirement**
   - Always use HTTPS in production
   - Tokens should be transmitted over encrypted channels only

### 🚀 Performance Considerations

1. **Database Calls**
   - JWT validation currently queries database for each request
   - Consider caching UserDetails for frequently accessed users
   - Use Redis for distributed caching in microservices

2. **Token Validation**
   - No database query needed if token cache is implemented
   - Only signature verification would be needed

3. **Scalability**
   - Stateless design allows horizontal scaling
   - No session affinity required
   - Any server instance can validate the token

### 🔧 Configuration Properties

```properties
# JWT Configuration
jwt.secret=your_base64_secret_key_here          # REQUIRED
jwt.expiration=3600000                          # 1 hour in milliseconds

# Hibernate Configuration
spring.jpa.show-sql=false                       # Set true for debugging
spring.hibernate.ddl-auto=update                # Auto-schema management

# Server Configuration
server.port=8080
server.servlet.context-path=/
```

### ⚠️ Common Pitfalls Fixed

1. **JWT Secret Missing `${}`**
   - ❌ `@Value("jwt.secret")` → Literal string, not property value
   - ✅ `@Value("${jwt.secret}")` → Correctly resolves from properties

2. **Missing @EnableMethodSecurity**
   - ❌ @PreAuthorize not enforced
   - ✅ Added to SecurityConfig

3. **Missing @Transactional on Delete**
   - ❌ `TransactionRequiredException` on delete operations
   - ✅ Added to deleteTask methods

4. **Circular Reference in JSON Serialization**
   - ❌ User → tasks → User (infinite loop)
   - ✅ Added @JsonIgnore on User.tasks
   - ✅ Use DTOs in API responses instead of entities

5. **Repository Method Names**
   - ❌ `findUserByEmail()` - custom naming
   - ✅ `findByEmail()` - Spring Data convention

### 📊 Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│                 STATELESS JWT AUTH FLOW                 │
└─────────────────────────────────────────────────────────┘

USER REGISTRATION
─────────────────
1. POST /api/auth/register → AuthService
2. Password hashed → BCryptPasswordEncoder
3. User saved to database
4. Response: 200 OK

USER LOGIN
──────────
1. POST /api/auth/login → AuthService
2. Credentials validated → AuthenticationManager
3. JWT created → JwtService (Claims: email, exp, iat, alg, sig)
4. Response: 200 OK + JWT Token

AUTHENTICATED REQUEST
─────────────────────
1. Client sends: GET /api/tasks + Authorization: Bearer JWT
2. JwtAuthenticationFilter intercepts
3. Token extracted and validated
4. User loaded from database
5. Token signature verified
6. Authentication set in SecurityContext
7. @PreAuthorize checks authorities
8. Method executed if authorized
9. Response returned with data/error

LOGOUT
──────
- No server-side logout needed (stateless)
- Client deletes token from local storage
- Token still valid until expiration
- Optional: Implement token blacklist for immediate invalidation
```

### 🔄 Transition from Stateful to Stateless

**What You Gained**:
- ✅ Horizontal scalability (no session affinity)
- ✅ Microservices friendly (independent token validation)
- ✅ Mobile/SPA friendly (token in headers)
- ✅ Reduced server memory usage
- ✅ Better API design

**What You Lost**:
- ❌ Cannot revoke active sessions instantly (JWT valid until expiration)
- ❌ Token size in headers (minimal impact)
- ❌ No server-side session state (implement blacklist if needed)

### 🛠️ Future Improvements

1. Implement **Refresh Tokens** for better UX
2. Add **Token Blacklist** for immediate logout
3. Implement **Rate Limiting** on auth endpoints
4. Add **API Key** authentication for service-to-service
5. Implement **OAuth2/OIDC** for social login
6. Add **2FA** (Two-Factor Authentication)
7. Use **Keycloak** or **Auth0** for centralized auth

---

## Testing the Stateless Configuration

### Test All Endpoints
```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"Test@123"}'

# 2. Login (get token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"Test@123"}'

# 3. Create Task (with token)
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"headLine":"Test","description":"Test task"}'

# 4. Get Tasks
curl -X GET http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 5. Delete Task
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

**Document Version**: 1.0  
**Last Updated**: February 17, 2026  
**Framework**: Spring Boot 4.0.2 with Spring Security 7.0.2
