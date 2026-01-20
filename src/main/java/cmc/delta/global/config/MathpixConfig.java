package cmc.delta.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {}
