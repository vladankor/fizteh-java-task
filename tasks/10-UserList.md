## UserList
GUI-приложение для просмотра-редактирования списка пользователей.

Используя наработки задания [XmlBinder](https://github.com/dkomanov/fizteh-java-task/blob/master/tasks/07-XmlBinder.md)
надо написать приложение на Swing, которое будет загружать из XML-файла список
пользователей: [User](https://github.com/dkomanov/fizteh-java-task/blob/master/src/ru/fizteh/fivt/bind/test/User.java),
отображать этот список, позволять редактировать этот список (добавлять новых
пользователей, редактировать атрибуты существующих и удалять) и сохранять либо
в исходный файл, либо в другой.

Программа должна корректно работать со списком из десяти тысяч пользователей.

### Вариант 1
При выборе в списке пользователя, должны подсвечиваться пользователи
с таким же именем.

### Вариант 2
Реализовать сортировку пользователей по имени и типу (по возрастанию и убыванию).

### Вариант 3
Реализовать диалог поиска пользователя по имени.

### Пример XML-файла
```(xml)
<users>
    <user>
        <id>1</id>
        <userType>MODERATOR</userType>
        <name>
            <firstName>root</firstName>
            <lastName>root</lastName>
        </name>
        <permissions>
            <root>true</root>
            <quota>100500</quota>
        </permissions>
    </user>
    <user>
        <id>2</id>
        <userType>USER</userType>
        <name>
            <firstName>Ivan</firstName>
            <lastName>Ivanov</lastName>
        </name>
        <permissions>
            <root>false</root>
            <quota>100</quota>
        </permissions>
    </user>
</users>
```

Т.е. корневой элемент ```<users>```, который содержит N-элементов ```<user>```,
которые представляют класс User. Могут встречаться и другие элементы.
