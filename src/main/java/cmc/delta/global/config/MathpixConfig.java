package cmc.delta.global.config;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {}
