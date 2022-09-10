package com.ytrue.orm.builder;

import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.mapping.ResultMapping;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/31 16:36
 * @description ResultMapResolver
 */
public class ResultMapResolver {

    private final MapperBuilderAssistant assistant;
    private String id;
    private Class<?> type;
    private List<ResultMapping> resultMappings;

    public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, List<ResultMapping> resultMappings) {
        this.assistant = assistant;
        this.id = id;
        this.type = type;
        this.resultMappings = resultMappings;
    }

    public ResultMap resolve() {
        return assistant.addResultMap(this.id, this.type, this.resultMappings);
    }

}
