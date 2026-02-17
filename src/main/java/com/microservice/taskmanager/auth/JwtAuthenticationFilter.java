package com.microservice.taskmanager.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomerUserDetailsService userDetailsService ;
    @Override
    @NullMarked
    protected void doFilterInternal( HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain ) throws ServletException, IOException
    {
        String authHeader = request.getHeader( "Authorization" );
        if ( authHeader == null || !authHeader.startsWith( "Bearer " ) ) {
            filterChain.doFilter( request, response );
        }
        assert authHeader != null;
        String jwt = authHeader.substring( 7 );
        String userName = jwtService.extractUserName(jwt);

        if ( userName !=null && SecurityContextHolder.getContext().getAuthentication() == null ) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

            if ( jwtService.isTokenValid(jwt,userDetails) ){
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter( request, response );
    }
}
