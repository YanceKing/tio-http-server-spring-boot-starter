package com.yance.routes;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import com.yance.annotation.TioAutowired;
import com.yance.configuration.TioSpringApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.server.annotation.RequestPath;
import org.tio.http.server.mvc.DefaultControllerFactory;
import org.tio.http.server.mvc.PathUnitVo;
import org.tio.http.server.mvc.Routes;
import org.tio.http.server.mvc.VariablePathVo;
import org.tio.http.server.mvc.intf.ControllerFactory;
import org.tio.utils.hutool.*;
import org.tio.utils.json.Json;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 自定义了路由规则，主要是重写了路由方法，将spring容器对象注入到Tio的控制器中
 *
 * @author yance
 */
public class CustomRoutes extends Routes {
    private static Logger log = LoggerFactory.getLogger(CustomRoutes.class);
    private StringBuilder errorStr;
    private TioSpringApplication tioSpring;

    public CustomRoutes(String[] scanPackages, TioSpringApplication tioSpring) {
        super(scanPackages, null);
        this.errorStr = new StringBuilder();
        this.tioSpring = tioSpring;
        this.addRoutes(scanPackages, null);
    }

    @Override
    public void addRoutes(String[] scanPackages, ControllerFactory controllerFactory) {
        if (controllerFactory == null) {
            controllerFactory = DefaultControllerFactory.me;
        }
        //判断是否为空，为空直接返回，调用父类路由必定为null，所以做了以下判断
        if (this.tioSpring == null) {
            return;
        }

        final ControllerFactory factory = controllerFactory;
        if (scanPackages != null) {
            String[] var4 = scanPackages;
            int var5 = scanPackages.length;
            String pkg;
            for (int var6 = 0; var6 < var5; ++var6) {
                pkg = var4[var6];
                try {
                    ClassUtil.scanPackage(pkg, new ClassScanAnnotationHandler(RequestPath.class) {
                        @Override
                        public void handlerAnnotation(Class<?> clazz) {
                            try {
                                Object bean = factory.getInstance(clazz);
                                RequestPath classMapping = clazz.getAnnotation(RequestPath.class);
                                String beanPath = classMapping.value();
                                Object obj = CustomRoutes.this.PATH_BEAN_MAP.get(beanPath);
                                if (obj != null) {
                                    CustomRoutes.log.error("mapping[{}] already exists in class [{}]", beanPath, obj.getClass().getName());
                                    CustomRoutes.this.errorStr.append("mapping[" + beanPath + "] already exists in class [" + obj.getClass().getName() + "]\r\n\r\n");
                                } else {
                                    CustomRoutes.this.PATH_BEAN_MAP.put(beanPath, bean);
                                    Routes.CLASS_BEAN_MAP.put(clazz, bean);
                                    CustomRoutes.this.PATH_CLASS_MAP.put(beanPath, clazz);
                                    Routes.CLASS_PATH_MAP.put(clazz, beanPath);
                                    MethodAccess access = MethodAccess.get(clazz);
                                    Routes.BEAN_METHODACCESS_MAP.put(bean, access);
                                }
                                //重写路由匹配规则主要代码  开始
                                Field[] fields = clazz.getDeclaredFields();
                                //查找字段中含有依赖注入的字段 存在就进行注入
                                for (Field field : fields) {
                                    TioAutowired inject = field.getAnnotation(TioAutowired.class);
                                    if (inject != null) {
                                        field.setAccessible(true);
                                        //1、注入的Bean
                                        Class<?> classBean = field.getType();
                                        Object object = tioSpring.getTioBean(classBean);
                                        field.set(bean, object);
                                    }
                                }
                                //重写路由匹配规则主要代码  结束
                                Method[] methods = clazz.getDeclaredMethods();
                                Method[] var7 = methods;
                                int var8 = methods.length;
                                for (int var9 = 0; var9 < var8; ++var9) {
                                    Method method = var7[var9];
                                    int modifiers = method.getModifiers();
                                    if (Modifier.isPublic(modifiers)) {
                                        RequestPath mapping = (RequestPath) method.getAnnotation(RequestPath.class);
                                        if (mapping != null) {
                                            String methodPath = mapping.value();
                                            String completePath = beanPath + methodPath;
                                            Class[] parameterTypes = method.getParameterTypes();
                                            try {
                                                Paranamer paranamer = new BytecodeReadingParanamer();
                                                String[] parameterNames = paranamer.lookupParameterNames(method, false);
                                                Method checkMethod = CustomRoutes.this.PATH_METHOD_MAP.get(completePath);
                                                if (checkMethod != null) {
                                                    CustomRoutes.log.error("mapping[{}] already exists in method [{}]", completePath, checkMethod.getDeclaringClass() + "#" + checkMethod.getName());
                                                    CustomRoutes.this.errorStr.append("mapping[" + completePath + "] already exists in method [" + checkMethod.getDeclaringClass() + "#" + checkMethod.getName() + "]\r\n\r\n");
                                                } else {
                                                    CustomRoutes.this.PATH_METHOD_MAP.put(completePath, method);
                                                    String methodStr = CustomRoutes.this.methodToStr(method, parameterNames);
                                                    CustomRoutes.this.PATH_METHODSTR_MAP.put(completePath, methodStr);
                                                    CustomRoutes.this.METHOD_PARAMNAME_MAP.put(method, parameterNames);
                                                    CustomRoutes.this.METHOD_PARAMTYPE_MAP.put(method, parameterTypes);
                                                    if (StrUtil.isNotBlank(mapping.forward())) {
                                                        CustomRoutes.this.PATH_FORWARD_MAP.put(completePath, mapping.forward());
                                                        CustomRoutes.this.PATH_METHODSTR_MAP.put(mapping.forward(), methodStr);
                                                        CustomRoutes.this.PATH_METHOD_MAP.put(mapping.forward(), method);
                                                    }
                                                    CustomRoutes.this.METHOD_BEAN_MAP.put(method, bean);
                                                }
                                            } catch (Throwable var20) {
                                                CustomRoutes.log.error(var20.toString(), var20);
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable var21) {
                                CustomRoutes.log.error(var21.toString(), var21);
                            }
                        }
                    });
                } catch (Exception var10) {
                    log.error(var10.toString(), var10);
                }
            }

            String pathClassMapStr = Json.toFormatedJson(this.PATH_CLASS_MAP);
            log.info("class  mapping\r\n{}", pathClassMapStr);
            String pathMethodstrMapStr = Json.toFormatedJson(this.PATH_METHODSTR_MAP);
            log.info("method mapping\r\n{}", pathMethodstrMapStr);
            this.processVariablePath();
            String variablePathMethodstrMapStr = Json.toFormatedJson(this.VARIABLEPATH_METHODSTR_MAP);
            log.info("variable path mapping\r\n{}", variablePathMethodstrMapStr);
            pkg = System.getProperty("tio.mvc.route.writeMappingToFile", "true");
            if ("true".equalsIgnoreCase(pkg)) {
                try {
                    FileUtil.writeString(pathClassMapStr, "/tio_mvc_path_class.json", "utf-8");
                    FileUtil.writeString(pathMethodstrMapStr, "/tio_mvc_path_method.json", "utf-8");
                    FileUtil.writeString(variablePathMethodstrMapStr, "/tio_mvc_variablepath_method.json", "utf-8");
                    if (this.errorStr.length() > 0) {
                        FileUtil.writeString(this.errorStr.toString(), "/tio_error_mvc.txt", "utf-8");
                    }
                } catch (Exception var9) {
                }
            }
        }

    }

    private void processVariablePath() {
        Set<Map.Entry<String, Method>> set = this.PATH_METHOD_MAP.entrySet();
        Iterator var2 = set.iterator();
        while (true) {
            String path;
            Method method;
            do {
                do {
                    if (!var2.hasNext()) {
                        return;
                    }
                    Map.Entry<String, Method> entry = (Map.Entry) var2.next();
                    path = entry.getKey();
                    method = entry.getValue();
                } while (!StrUtil.contains(path, '{'));
            } while (!StrUtil.contains(path, '}'));

            String[] pathUnits = StrUtil.split(path, "/");
            PathUnitVo[] pathUnitVos = new PathUnitVo[pathUnits.length];
            boolean isVarPath = false;

            for (int i = 0; i < pathUnits.length; ++i) {
                PathUnitVo pathUnitVo = new PathUnitVo();
                String pathUnit = pathUnits[i];
                if (!StrUtil.contains(pathUnit, '{') && !StrUtil.contains(pathUnit, '}')) {
                    pathUnitVo.setVar(false);
                    pathUnitVo.setPath(pathUnit);
                } else if (StrUtil.startWith(pathUnit, "{") && StrUtil.endWith(pathUnit, "}")) {
                    String[] xx = (String[]) this.METHOD_PARAMNAME_MAP.get(method);
                    String varName = StrUtil.subBetween(pathUnit, "{", "}");
                    if (ArrayUtil.contains(xx, varName)) {
                        isVarPath = true;
                        pathUnitVo.setVar(true);
                        pathUnitVo.setPath(varName);
                    } else {
                        log.error("path:{}, 对应的方法中并没有包含参数名为{}的参数", path, varName);
                        this.errorStr.append("path:{" + path + "}, 对应的方法中并没有包含参数名为" + varName + "的参数\r\n\r\n");
                    }
                } else {
                    pathUnitVo.setVar(false);
                    pathUnitVo.setPath(pathUnit);
                }
                pathUnitVos[i] = pathUnitVo;
            }
            if (isVarPath) {
                VariablePathVo variablePathVo = new VariablePathVo(path, method, pathUnitVos);
                this.addVariablePathVo(pathUnits.length, variablePathVo);
            }
        }
    }

    private String methodToStr(Method method, String[] parameterNames) {
        return method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.join(parameterNames, ",") + ")";
    }

    private void addVariablePathVo(Integer pathUnitCount, VariablePathVo variablePathVo) {
        VariablePathVo[] existValue = (VariablePathVo[]) this.VARIABLE_PATH_MAP.get(pathUnitCount);
        if (existValue == null) {
            existValue = new VariablePathVo[]{variablePathVo};
            this.VARIABLE_PATH_MAP.put(pathUnitCount, existValue);
        } else {
            VariablePathVo[] newExistValue = new VariablePathVo[existValue.length + 1];
            System.arraycopy(existValue, 0, newExistValue, 0, existValue.length);
            newExistValue[newExistValue.length - 1] = variablePathVo;
            this.VARIABLE_PATH_MAP.put(pathUnitCount, newExistValue);
        }
        this.VARIABLEPATH_METHODSTR_MAP.put(variablePathVo.getPath(), this.methodToStr(variablePathVo.getMethod(), (String[]) this.METHOD_PARAMNAME_MAP.get(variablePathVo.getMethod())));
    }
}
