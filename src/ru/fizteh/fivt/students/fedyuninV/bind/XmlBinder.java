package ru.fizteh.fivt.students.fedyuninV.bind;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.fizteh.fivt.bind.AsXmlElement;
import ru.fizteh.fivt.bind.BindingType;
import ru.fizteh.fivt.bind.MembersToBind;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fedyunin Valeriy
 * MIPT FIVT 195
 */
public class XmlBinder<T> extends ru.fizteh.fivt.bind.XmlBinder<T>{


    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    public XmlBinder(Class<T> clazz) {
        super(clazz);
    }


    private boolean checkPrefix(String prefix, String methodName) {
        return (methodName.length() >= prefix.length()  &&  methodName.substring(0, prefix.length()).equals(prefix));
    }

    private String firstCharToLowerCase(String str) {
        char first = str.charAt(0);
        if (first >= 'A'  &&  first <= 'Z') {
            StringBuilder builder = new StringBuilder();
            builder.append((char) (first - 'A' + 'a'));
            builder.append(str.substring(1));
            return builder.toString();
        }
        return str;
    }

    private String getElementName(Object value, String defaultName) {
        AsXmlElement asXmlElement = value.getClass().getAnnotation(AsXmlElement.class);
        if (asXmlElement == null) {
            return firstCharToLowerCase(defaultName);
        } else {
            return asXmlElement.name();
        }
    }
    private boolean possibleToString(Class classExample) {
        return (classExample.isPrimitive()
                ||  classExample.getName().equals("java.lang.Integer")
                ||  classExample.getName().equals("java.lang.Boolean")
                ||  classExample.getName().equals("java.lang.String")
                ||  classExample.getName().equals("java.lang.Double")
                ||  classExample.getName().equals("java.lang.Float")
                || classExample.getName().equals("java.lang.Byte")
                ||  classExample.getName().equals("java.lang.Long")
                ||  classExample.getName().equals("java.lang.Short")
                ||  classExample.isEnum());
    }

    private void writeToDocumentByFields(Document document, Object value, Element root) throws Exception {
        Set<String> alreadyWritten = new TreeSet<>();
        Class parent = value.getClass();
        while (parent != null) {
            Field[] fields = value.getClass().getDeclaredFields();
            for (Field field: fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                if (fieldValue != null  &&  !alreadyWritten.contains(field.getName())) {
                    alreadyWritten.add(field.getName());
                    Element child = document.createElement(field.getName());
                    root.appendChild(child);
                    if (possibleToString(fieldValue.getClass())) {
                        child.setTextContent(fieldValue.toString());
                    } else {
                        writeToDocument(document, fieldValue, child);
                    }
                }
            }
            parent = parent.getSuperclass();
        }
    }

    private void writeToDocumentByMethods(Document document, Object value, Element root) throws Exception {
        //building components to Bind
        List<SerializeComponent> components = new ArrayList<>();
        Method[] methods = value.getClass().getMethods();
        for (Method method: methods) {
            String name = null;
            char mode = 'g';
            Class[] args = method.getParameterTypes();
            String methodName = method.getName();
            if (checkPrefix("get", methodName)  &&  args.length == 0) {
                name = methodName.substring(3);
            }
            if (checkPrefix("is", methodName)  &&  args.length == 0) {
                name = methodName.substring(2);
            }
            if (checkPrefix("set", methodName)  &&  args.length == 1) {
                name = methodName.substring(3);
                mode = 's';
            }
            if (name  != null) {
                boolean componentFound = false;
                for (SerializeComponent component: components) {
                    if (component.getName().equals(name)) {
                        componentFound = true;
                        if (!component.setMethod(method, mode)) {
                            throw new RuntimeException("Indefinite pair of methods found in " + value.getClass().getName());
                        }
                    }
                }
                if (!componentFound) {
                    SerializeComponent newComponent = new SerializeComponent(name);
                    if (!newComponent.setMethod(method, mode)) {
                        throw new RuntimeException("Error while creating new component");
                    }
                    components.add(newComponent);
                }
            }
        }
        for (SerializeComponent component: components) {
            //System.out.println(component.getName() + " " + (component.getter() == null) + " " + (component.setter() == null));
            if (component.getter() != null  &&  component.setter() != null) {
                String name = firstCharToLowerCase(component.getName());
                Object newValue = component.getter().invoke(value);
                if (newValue != null) {
                    Element child = document.createElement(name);
                    root.appendChild(child);
                    if (possibleToString(newValue.getClass())) {
                        child.setTextContent(newValue.toString());
                    } else {
                        writeToDocument(document, newValue, child);
                    }
                }
                //document.appendChild(child);
            }
        }
    }


    private void writeToDocument(Document document, Object value, Element root) throws Exception{
        BindingType bindingType = value.getClass().getAnnotation(BindingType.class);
        if (bindingType == null  ||  bindingType.value().equals(MembersToBind.FIELDS)) {
            writeToDocumentByFields(document, value, root);
        } else {
            writeToDocumentByMethods(document, value, root);
        }
    }



    @Override
    public byte[] serialize(Object value) {
        if (!value.getClass().equals(getClazz())) {
            throw new RuntimeException("This class is not supported by this binder!");
        }

        //Creating XML
        try {
            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement(getElementName(value, value.getClass().getName()));
            writeToDocument(document, value, root);
            document.appendChild(root);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Result result = new StreamResult(out);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount",
                    "2"
            );
            transformer.transform(new DOMSource(document), result);
            return out.toByteArray();
            /*StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            System.out.println(stringWriter.getBuffer().toString());
            return null;*/
        } catch (Exception ex) {
            throw new RuntimeException("Serializing error: ", ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
