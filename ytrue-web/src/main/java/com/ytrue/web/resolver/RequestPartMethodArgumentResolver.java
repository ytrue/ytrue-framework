package com.ytrue.web.resolver;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.multipart.MultipartFile;
import com.ytrue.web.multipart.StandardMultipartFile;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 13:59
 * @description RequestPartMethodArgumentResolver
 */
public class RequestPartMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return isMultipartFile(parameter);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        // 拿到当前参数的名称和
        final String parameterName = parameter.getParameterName();
        // 获取文件
        final HttpServletRequest request = webServletRequest.getRequest();
        final Collection<Part> parts = request.getParts();
        final ArrayList<MultipartFile> files = new ArrayList<>();
        for (Part part : parts) {
            if (parameterName.equals(part.getName())) {
                if (!ObjectUtils.isEmpty(part)) {
                    final StandardMultipartFile standardMultipartFile = new StandardMultipartFile(part, part.getSubmittedFileName());
                    files.add(standardMultipartFile);
                }
            }
        }
        final Class<?> parameterType = parameter.getParameterType();
        if (parameterType == MultipartFile.class) {
            return files.get(0);
        } else if (parameterType == List.class || parameterType == Collection.class) {
            return files;
        } else if (parameterType.isArray()) {
            return files.toArray(new MultipartFile[files.size()]);
        }
        return null;
    }


    // 单个/集合
    public static boolean isMultipartFile(MethodParameter parameter) {

        final Class<?> parameterType = parameter.getParameterType();
        if (parameterType == MultipartFile.class) {
            return true;
        }

        if (parameterType == List.class || parameterType == Collection.class) {
            // 获取集合中的泛型是否是MultipartFile []
            final Type genericParameterType = parameter.getGenericParameterType();
            ParameterizedType type = (ParameterizedType) genericParameterType;

            if (type.getActualTypeArguments()[0] == MultipartFile.class) {
                return true;
            }
        }
        if (parameterType.isArray() && parameterType == MultipartFile.class) {
            return true;
        }

        return false;
    }

}
