package ru.fizteh.fivt.students.tolyapro.xmlBinder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.fizteh.fivt.bind.AsXmlElement;
import sun.misc.Unsafe;

public class XmlBinder<T> extends ru.fizteh.fivt.bind.XmlBinder<T> {
    IdentityHashMap<Class, ArrayList<Field>> fieldsToSerialize;
    IdentityHashMap<Class, ArrayList<GetterAndSetter>> methodsToSerialize;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    XMLEventWriter eventWriter;
    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    XMLEvent end = eventFactory.createDTD("\n");
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader;
    String selfName;
    Unsafe unsafeInstance;

    protected XmlBinder(Class<T> clazz) {
        super(clazz);
        fieldsToSerialize = new IdentityHashMap<Class, ArrayList<Field>>();
        methodsToSerialize = new IdentityHashMap<Class, ArrayList<GetterAndSetter>>();
        selfName = getName(clazz);
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafeInstance = Unsafe.class.cast(field.get(null));
            eventWriter = xmlOutputFactory
                    .createXMLEventWriter(byteArrayOutputStream);
        } catch (Exception e) {
            throw new RuntimeException("Can't create XmlBinder");
        }
    }

    private static boolean isWrapperType(Class clazz) {
        return clazz.equals(Boolean.class) || clazz.equals(Integer.class)
                || clazz.equals(Character.class) || clazz.equals(Byte.class)
                || clazz.equals(Short.class) || clazz.equals(Double.class)
                || clazz.equals(Long.class) || clazz.equals(Float.class);
    }

    private static boolean isPrimitive(Class clazz) {
        return clazz.isPrimitive() || clazz.isEnum()
                || clazz.equals(String.class) || isWrapperType(clazz);
    }

    private void createNode(String name, String value)
            throws XMLStreamException {
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        XMLEvent tab = eventFactory.createDTD("\t");
        StartElement sElement = eventFactory.createStartElement("", "", name);
        eventWriter.add(tab);
        eventWriter.add(sElement);
        Characters characters = eventFactory.createCharacters(value);
        eventWriter.add(characters);
        EndElement eElement = eventFactory.createEndElement("", "", name);
        eventWriter.add(eElement);
        eventWriter.add(end);
    }

    private void pushAllFields(Object value) {
        Field[] tmp = value.getClass().getDeclaredFields();
        ArrayList<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(tmp));
        Class parent = value.getClass().getSuperclass();
        while (parent != null) {
            tmp = parent.getDeclaredFields();
            fields.addAll(Arrays.asList(tmp));
            parent = parent.getSuperclass();
        }
        fieldsToSerialize.put(value.getClass(), fields);
    }

    private void pushAllFields(Class clazz) {
        Field[] tmp = clazz.getDeclaredFields();
        ArrayList<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(tmp));
        Class parent = clazz.getSuperclass();
        while (parent != null) {
            tmp = parent.getDeclaredFields();
            fields.addAll(Arrays.asList(tmp));
            parent = parent.getSuperclass();
        }
        fieldsToSerialize.put(clazz, fields);
    }

    private void pushAllMethods(Class valueClass) throws Exception {
        Method[] tmp = valueClass.getDeclaredMethods();
        ArrayList<Method> methods = new ArrayList<Method>();
        methods.addAll(Arrays.asList(tmp));
        Class parent = valueClass.getSuperclass();
        while (parent != null) {
            tmp = parent.getDeclaredMethods();
            methods.addAll(Arrays.asList(tmp));
            parent = parent.getSuperclass();
        }
        ArrayList<GetterAndSetter> newMethods = new ArrayList<GetterAndSetter>();
        // split it to the GetterAndSetter's;
        try {
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.matches("set.+")) {
                    String getterName = methodName.replaceFirst("set", "get");
                    try {
                        Method getter = valueClass
                                .getDeclaredMethod(getterName);
                        if (!method.getParameterTypes()[0].equals(getter
                                .getReturnType())) {
                            continue;
                        }
                        GetterAndSetter getterAndSetter = new GetterAndSetter();
                        getterAndSetter.getter = getter;
                        getterAndSetter.setter = method;
                        getterAndSetter.getter.setAccessible(true);
                        getterAndSetter.setter.setAccessible(true);
                        String name = null;
                        if (getterName.length() - 3 > 1) {
                            name = getterName.replaceFirst("get", "");
                            name = Character.toLowerCase(name.charAt(0))
                                    + name.substring(1);
                        } else {
                            name = getterName.replaceFirst("get", "");
                            Character c = name.charAt(0);
                            name = Character.toString(Character.toLowerCase(c));
                        }
                        getterAndSetter.name = name;
                        getterAndSetter.type = getter.getReturnType();
                        newMethods.add(getterAndSetter);
                    } catch (Exception e) {

                    }
                    String isName = methodName.replaceFirst("set", "is");
                    try {
                        Method getter = valueClass.getDeclaredMethod(isName);
                        if (!method.getParameterTypes()[0].equals(getter
                                .getReturnType())) {
                            continue;
                        }
                        GetterAndSetter getterAndSetter = new GetterAndSetter();
                        getterAndSetter.getter = getter;
                        getterAndSetter.setter = method;
                        getterAndSetter.getter.setAccessible(true);
                        getterAndSetter.setter.setAccessible(true);
                        String name = null;
                        if (isName.length() - 3 > 1) {
                            name = isName.replaceFirst("get", "");
                            name = Character.toLowerCase(name.charAt(0))
                                    + name.substring(1);
                        } else {
                            name = isName.replaceFirst("get", "");
                            Character c = name.charAt(0);
                            name = Character.toString(Character.toLowerCase(c));
                        }
                        // #TODO create method firstLetterLowerer
                        getterAndSetter.name = name;
                        getterAndSetter.type = getter.getReturnType();
                        newMethods.add(getterAndSetter);
                    } catch (Exception e) {

                    }
                }
            }
            methodsToSerialize.put(valueClass, newMethods);
        } catch (Exception e) {
            throw new Exception("Troubles with parsing methods", e.getCause());
        }

    }

    public void serializeClass(T value, String name) throws Exception {
        pushAllFields(value);
        pushAllMethods(value.getClass());
        serializeMe(value.getClass(), value, getName(value.getClass()));
    }

    public void serializeMe(Class clazz, Object value, String title)
            throws Exception {
        boolean serializeMethods = false;
        ru.fizteh.fivt.bind.BindingType annotation = (ru.fizteh.fivt.bind.BindingType) clazz
                .getAnnotation(ru.fizteh.fivt.bind.BindingType.class);
        if (annotation == null) {

        } else if (annotation.value().equals(
                ru.fizteh.fivt.bind.MembersToBind.GETTERS_AND_SETTERS)) {
            serializeMethods = true;
        }
        if (!serializeMethods) {
            if (fieldsToSerialize.containsKey(clazz)) {
                StartElement configStartElement = eventFactory
                        .createStartElement("", "", title);
                eventWriter.add(configStartElement);
                eventWriter.add(end);
                ArrayList<Field> fields = fieldsToSerialize.get(clazz);
                for (int i = 0; i < fields.size(); ++i) {
                    Field tmpField = fields.get(i);
                    tmpField.setAccessible(true);
                    Object obj = null;
                    try {
                        obj = tmpField.get(value);
                    } catch (Exception e) {
                        continue;
                    }
                    if (obj == null) {
                        continue;
                    }
                    if (!isPrimitive(tmpField.getType())) {
                        if (!fieldsToSerialize.containsKey(obj.getClass())) {
                            pushAllFields(obj);
                            pushAllMethods(obj.getClass());
                        }
                        serializeMe(obj.getClass(), obj, tmpField.getName());
                    } else {
                        AsXmlElement asXmlElement = tmpField
                                .getAnnotation(ru.fizteh.fivt.bind.AsXmlElement.class);
                        if (asXmlElement == null || asXmlElement.name() == null
                                || asXmlElement.name() == "") {
                            createNode(tmpField.getName(), obj.toString());
                        } else {
                            createNode(asXmlElement.name(), obj.toString());
                        }

                    }
                }
                eventWriter.add(eventFactory.createEndElement("", "", title));
                eventWriter.add(end);
            } else {
                throw new Exception("No class in fieldsToSerialize");
            }
        } else {
            if (methodsToSerialize.containsKey(clazz)) {
                StartElement configStartElement = eventFactory
                        .createStartElement("", "", title);
                eventWriter.add(configStartElement);
                eventWriter.add(end);
                ArrayList<GetterAndSetter> methods = methodsToSerialize
                        .get(clazz);
                for (int i = 0; i < methods.size(); ++i) {
                    Object obj = methods.get(i).getter.invoke(value);
                    if (obj == null) {
                        continue;
                    }
                    if (!isPrimitive(methods.get(i).type)) {
                        pushAllMethods(obj.getClass());
                        serializeMe(obj.getClass(), obj, methods.get(i).name);
                    } else {
                        AsXmlElement asXmlElement = methods.get(i).getter
                                .getAnnotation(ru.fizteh.fivt.bind.AsXmlElement.class);
                        if (asXmlElement == null || asXmlElement.name() == null
                                || asXmlElement.name() == "") {
                            createNode(methods.get(i).name, obj.toString());
                        } else {
                            createNode(asXmlElement.name(), obj.toString());
                        }
                    }
                }
                eventWriter.add(eventFactory.createEndElement("", "", title));
                eventWriter.add(end);
            }
        }
    }

    public String getName(Class clazz) {
        String name = clazz.getSimpleName().replaceFirst(
                clazz.getPackage().getName() + "\\.", "");
        try {
            if (name.length() > 1) {
                name = Character.toLowerCase(name.charAt(0))
                        + name.substring(1);
            } else {
                Character c = Character.toLowerCase(name.charAt(0));
                name = c.toString();
            }
        } catch (Exception e) {
            System.out.println("shit");
        }
        return name;
    }

    @Override
    public byte[] serialize(T value) {
        Class clazz = value.getClass();
        String name = getName(clazz);
        try {
            serializeClass(value, name);
            eventWriter.add(eventFactory.createEndDocument());
            eventWriter.close();
            byte[] result = byteArrayOutputStream.toByteArray();
            // System.out.println(new String(result));
            fieldsToSerialize.clear();
            methodsToSerialize.clear();
            byteArrayOutputStream.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Can't serialize", e.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(byte[] bytes) {
        try {
            if (bytes == null || bytes.length == 0) {
                throw new Exception("Bad bytes");
            }
            fieldsToSerialize = new IdentityHashMap<Class, ArrayList<Field>>();
            methodsToSerialize = new IdentityHashMap<Class, ArrayList<GetterAndSetter>>();
            Object obj = parse(bytes);
            return (T) obj;
        } catch (Exception e) {
            throw new RuntimeException("Can't deserialize", e.getCause());
        }
    }

    Object parse(byte[] bytes) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                bytes);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory
                .newInstance();
        Document document = builderFactory.newDocumentBuilder().parse(
                byteArrayInputStream);
        if (!document.getDocumentElement().getTagName().equals(selfName)) {
            throw new Exception("Bad XML doc");
        }
        pushAllFields(getClazz());
        pushAllMethods(getClazz());
        return deserializeMe(document.getDocumentElement(), getClazz());
    }

    public Object deserializePrimitive(Class clazz, String string)
            throws Exception {
        if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
            return Byte.parseByte(string);
        }
        if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            return Short.parseShort(string);
        }
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return Integer.parseInt(string);
        }
        if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return Long.parseLong(string);
        }
        if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return Double.parseDouble(string);
        }
        if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return Float.parseFloat(string);
        }
        if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return Boolean.parseBoolean(string);
        }
        if (clazz.equals(String.class)) {
            return string;
        }
        if (clazz.equals(char.class) || clazz.equals(Character.class)) {
            if (string.length() != 1) {
                throw new Exception("Not char");
            }
            return string.charAt(0);
        }
        if (clazz.isEnum()) {
            return Enum.valueOf(clazz, string);
        }
        return null;
    }

    @SuppressWarnings("restriction")
    public Object deserializeMe(Element element, Class clazz) throws Exception {
        if (isPrimitive(clazz)) {
            return deserializePrimitive(clazz, element.getTextContent());
        } else {
            if (!fieldsToSerialize.containsKey(clazz)) {
                pushAllFields(clazz);
                pushAllMethods(clazz);
            }
        }
        boolean deserializeMethods = false;
        ru.fizteh.fivt.bind.BindingType annotation = (ru.fizteh.fivt.bind.BindingType) clazz
                .getAnnotation(ru.fizteh.fivt.bind.BindingType.class);
        if (annotation == null) {

        } else if (annotation.value().equals(
                ru.fizteh.fivt.bind.MembersToBind.GETTERS_AND_SETTERS)) {
            deserializeMethods = true;
        }
        if (!deserializeMethods) {
            Object object = getNewInstance(clazz);
            NodeList nodeList = element.getChildNodes();
            if (!fieldsToSerialize.containsKey(clazz)) {
                throw new Exception("No such class");
            }
            ArrayList<Field> fields = fieldsToSerialize.get(clazz);
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                Field thisField = null;
                if (node == null) {
                    continue;
                }
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    for (Field field : fields) {
                        if (field.getName().equals(
                                ((Element) node).getTagName())) {
                            thisField = field;
                            break;
                        } else {
                            AsXmlElement asXmlElement = field
                                    .getAnnotation(AsXmlElement.class);
                            if (asXmlElement != null) {
                                if (asXmlElement.name().equals(
                                        ((Element) node).getTagName())) {
                                    thisField = field;
                                    break;
                                }
                            }
                        }
                    }
                    if (thisField == null) {
                        continue;
                    }
                    thisField.setAccessible(true);
                    thisField.set(object,
                            deserializeMe((Element) node, thisField.getType()));
                }
            }
            return object;
        } else {
            Object object = getNewInstance(clazz);
            NodeList nodeList = element.getChildNodes();
            if (!methodsToSerialize.containsKey(clazz)) {
                throw new Exception("No such class");
            }
            ArrayList<GetterAndSetter> methods = methodsToSerialize.get(clazz);
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    GetterAndSetter thisMethod = null;
                    for (GetterAndSetter method : methods) {
                        if (method.name.equals(((Element) node).getTagName())) {
                            thisMethod = method;
                            break;
                        } else {
                            AsXmlElement asXmlElement = method.setter
                                    .getAnnotation(AsXmlElement.class);
                            if (asXmlElement != null) {
                                if (asXmlElement.name().equals(
                                        ((Element) node).getTagName())) {
                                    thisMethod = method;
                                    break;
                                }
                            }
                        }
                    }
                    if (thisMethod == null) {
                        continue;
                    }
                    thisMethod.setter.invoke(object,
                            deserializeMe((Element) node, thisMethod.type));
                }
            }
            return object;
        }
    }

    Object getNewInstance(Class clazz) throws InstantiationException {
        try {
            Constructor constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return unsafeInstance.allocateInstance(clazz);
        }
    }
}
