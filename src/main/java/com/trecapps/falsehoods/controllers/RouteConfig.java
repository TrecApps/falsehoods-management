package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.Brand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.IStandardConversionService;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class RouteConfig implements WebFluxConfigurer
{
    @Autowired
    private BrandsRouter frontendRouter;

    @Autowired
    private LoginRouter loginRouter;

    @Autowired
    private FalsehoodsRouter falsehoodsRouter;

    @Autowired
    private ApplicationContext applicationContext;

//        @Autowired
//    ObjectMapper objectMapper;

//    @Autowired
//    JsonConversionService jsonConversionService;

    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions.route()
                .GET("/Welcome",frontendRouter::welcomePage)
                .GET("/Login", loginRouter::loginPage)
                .GET("/Article/{id}", frontendRouter::articlePage)
                .GET("/ArticleEdit", frontendRouter::articleEditPage)
                .GET("/ArticleEdit/{id}", frontendRouter::articleEditPage)
                .GET("/FalsehoodSearch", falsehoodsRouter::falsehoodSearchPage)
                .GET("/Falsehood/{id}", falsehoodsRouter::falsehoodPage)
                .build();
    }


    public ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");

//        templateResolver.setOrder(1);
        return templateResolver;
    }

    public ISpringWebFluxTemplateEngine templateEngine() {
        SpringWebFluxTemplateEngine templateEngine = new SpringWebFluxTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());

//        SpringStandardDialect dialect = new SpringStandardDialect();
//        dialect.setConversionService(jsonConversionService);
//
//        templateEngine.setDialect(dialect);

        return templateEngine;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        ThymeleafReactiveViewResolver viewResolver = new ThymeleafReactiveViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setApplicationContext(applicationContext);

        registry.viewResolver(viewResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images");
        registry
                .addResourceHandler("/md-images/**")
                .addResourceLocations("classpath:/static/md-images");
        registry
                .addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js");
        registry
                .addResourceHandler("/styles/**")
                .addResourceLocations("classpath:/static/styles");
}

}
