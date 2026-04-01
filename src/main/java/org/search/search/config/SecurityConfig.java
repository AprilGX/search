package org.search.search.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public HttpFirewall httpFirewall() {
        // 使用DefaultHttpFirewall代替StrictHttpFirewall，对URL限制少
        return new DefaultHttpFirewall();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable() // 关闭CSRF
                .formLogin().disable()
                .cors()           // 启用CORS
                .and()
                .authorizeRequests()
                // 事件绑定相关接口需要认证
                .antMatchers("/api/event/associate", "/api/event/associate/batch").authenticated()
                // 其他API路径无需认证
                .antMatchers(
                        "/",                    //首页
                        "/login",               //需要认证的路由会由此去主站登录验证
                        "/api/search/**",       //对内对外提供的方法
                        "/api/monitor/**",      //【新增】监控统计接口
                        "/api/dict/**",         //【关键】放行词典接口，供ES和IK分词器调用
                        "/sync/recreate-index", //删除原本索引，重新建立索引（设置定时任务）
                        "/api/event/list",       //获取事件列表无需认证
                        "/help.html",
                        "/search.html",
                        "/样式/**",
                        "/lib/**",
                        "/favicon.ico",

                        "/es/rebuild-paragraph-index"
                        ).permitAll() // 允许访问的路径

                .anyRequest().authenticated() // 其他所有请求需要认证
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 添加JWT过滤器
    }

    /**
     * 全局CORS过滤器，允许所有来源和方法
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // 支持任意域名，Spring 5.3+推荐使用addAllowedOriginPattern
        config.addAllowedHeader("*");         // 允许所有请求头
        config.addAllowedMethod("*");         // 允许所有请求方法
        config.setAllowCredentials(true);     // 支持携带cookie

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
