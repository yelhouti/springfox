/*
 *
 *  Copyright 2015-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.swagger.readers.parameter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.service.AllowableValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.EnumTypeDeterminer;
import springfox.documentation.spi.service.ExpandedParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterExpansionContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.schema.ApiModelProperties;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Optional.*;
import static com.google.common.base.Strings.*;
import static com.google.common.collect.Lists.*;
import static springfox.documentation.swagger.annotations.Annotations.*;
import static springfox.documentation.swagger.schema.ApiModelProperties.*;

@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class SwaggerExpandedParameterBuilder implements ExpandedParameterBuilderPlugin {
  private final DescriptionResolver descriptions;
  private EnumTypeDeterminer enumTypeDeterminer;

  @Autowired
  public SwaggerExpandedParameterBuilder(DescriptionResolver descriptions, EnumTypeDeterminer enumTypeDeterminer) {
    this.descriptions = descriptions;
    this.enumTypeDeterminer=enumTypeDeterminer;
  }

  @Override
  public void apply(ParameterExpansionContext context) {
    Optional<ApiModelProperty> apiModelPropertyOptional
        = findApiModePropertyAnnotation(context.getField().getRawMember());
    if (apiModelPropertyOptional.isPresent()) {
      fromApiModelProperty(context, apiModelPropertyOptional.get());
    }
    Optional<ApiParam> apiParamOptional = findApiParamAnnotation(context.getField().getRawMember());
    if (apiParamOptional.isPresent()) {
      fromApiParam(context, apiParamOptional.get());
    }
  }

  @Override
  public boolean supports(DocumentationType delimiter) {
    return SwaggerPluginSupport.pluginDoesApply(delimiter);
  }

  private void fromApiParam(ParameterExpansionContext context, ApiParam apiParam) {
    String allowableProperty = emptyToNull(apiParam.allowableValues());
    AllowableValues allowable = allowableValues(fromNullable(allowableProperty), context.getField().getRawMember());
    context.getParameterBuilder()
            .description(descriptions.resolve(apiParam.value()))
            .defaultValue(apiParam.defaultValue())
            .required(apiParam.required())
            .allowMultiple(apiParam.allowMultiple())
            .allowableValues(allowable)
            .parameterAccess(apiParam.access())
            .hidden(apiParam.hidden())
            .build();
  }

  private void fromApiModelProperty(ParameterExpansionContext context, ApiModelProperty apiModelProperty) {
    String allowableProperty = emptyToNull(apiModelProperty.allowableValues());
    AllowableValues allowable = allowableValues(fromNullable(allowableProperty), context.getField().getRawMember());
    context.getParameterBuilder()
            .description(descriptions.resolve(apiModelProperty.value()))
            .required(apiModelProperty.required())
            .allowableValues(allowable)
            .parameterAccess(apiModelProperty.access())
            .hidden(apiModelProperty.hidden())
            .build();
  }

  private AllowableValues allowableValues(final Optional<String> optionalAllowable, final Field field) {

    AllowableValues allowable = null;
    if (enumTypeDeterminer.isEnum(field.getType())) {
      allowable = new AllowableListValues(getEnumValues(field.getType()), "LIST");
    } else if (optionalAllowable.isPresent()) {
      allowable = ApiModelProperties.allowableValueFromString(optionalAllowable.get());
    }
    return allowable;
  }

  private List<String> getEnumValues(final Class<?> subject) {
    return transform(Arrays.asList(subject.getEnumConstants()), new Function<Object, String>() {
      @Override
      public String apply(final Object input) {
        return input.toString();
      }
    });
  }
}
