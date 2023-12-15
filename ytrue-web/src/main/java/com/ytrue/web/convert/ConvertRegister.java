//package com.ytrue.web.convert;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
///**
// * @author ytrue
// * @date 2023-12-15 11:38
// * @description ConvertRegister
// */
//public class ConvertRegister {
//
//    private List<Convert> convertList = new ArrayList<>();
//
//
//    public void addConvert(Convert convert) {
//        this.convertList.add(convert);
//    }
//
//
//    public Map<Class, ConvertHandler> getConverts() {
//        final HashMap<Class, ConvertHandler> convertHandlerHashMap = new HashMap<>();
//        for (Convert convert : this.convertList) {
//            final Class type = convert.getType();
//            try {
//                final Method method = convert.getClass().getDeclaredMethod("convert", String.class);
//                convertHandlerHashMap.put(type, new ConvertHandler(convert, method));
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//
//        }
//        return convertHandlerHashMap;
//    }
//
//
//}
//
