package com.wimroukema.hydraserver.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration

public class ProjectSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf((csrf) -> csrf.disable());
		http.cors(Customizer.withDefaults());
		http.authorizeHttpRequests((requests) -> requests.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/status").authenticated().requestMatchers("/startProcess").authenticated()
				.requestMatchers("/stopProcess").authenticated().requestMatchers("/stopRelay").authenticated()
				.requestMatchers("/removeRelay").authenticated().requestMatchers("/clearProcess").authenticated()
				.requestMatchers("/actives").authenticated().requestMatchers("/repeatProcess").authenticated()
				.requestMatchers("/addRelays").authenticated().requestMatchers("/actuallog").authenticated()
				.requestMatchers("/otherlog").authenticated().requestMatchers("/getBatchTimes").authenticated()
				.requestMatchers("/postBatchTimes").authenticated().requestMatchers("/backgroundProcess")
				.authenticated().requestMatchers("/getRelays").authenticated().requestMatchers("/storeRelays")
				.authenticated());
		http.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	InMemoryUserDetailsManager userDetailsService() {

		UserDetails admin = User.withUsername("wim").password(passwordEncoder().encode("abc123wim")).roles("USER")
				.build();

		return new InMemoryUserDetailsManager(admin);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(List.of("http://localhost:41161", "https://wimroukema.nl", "https://vps.wimroukema.nl",
				"https://linode.wimroukema.nl", "https://tennis.wimroukema.nl"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
