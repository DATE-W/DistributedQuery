import javax.xml.stream.*

// 定义切分块数
pieces = 6
// 定义XML源文件路径
input = "D:\\sthgithub\\DistributedQuery\\dblp.xml"
// 定义输出文件的命名格式
output = ".\\output_%04d.xml"
// 创建XML事件工厂实例
eventFactory = XMLEventFactory.newInstance()
// 初始化文件编号和元素计数
fileNumber = elementCount = 0
// 设置系统属性以增加XML实体展开限制，防止大文件处理时的异常
System.setProperty("entityExpansionLimit", "3000000")

// 创建事件读取器函数
def createEventReader() {
    reader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(input))

    // 读取第一个事件，跳过可能的异常
    while (true) {
        try {
            start = reader.next()
        } catch (XMLStreamException e) {
            continue
        }
        break
    }

    // 读取根标签
    while (true) {
        try {
            root = reader.nextTag()
        } catch (XMLStreamException e) {
            continue
        }
        break
    }

    // 读取根标签的第一个子标签
    while (true) {
        try {
            firstChild = reader.nextTag()
        } catch (XMLStreamException e) {
            continue
        }
        break
    }
    return reader
}

// 创建下一个事件写入器函数
def createNextEventWriter() {
    println "Writing to '${filename = String.format(output, ++fileNumber)}'"
    writer = XMLOutputFactory.newInstance().createXMLEventWriter(new FileOutputStream(filename), start.characterEncodingScheme)
    writer.add(start)
    writer.add(root)
    return writer
}

// 获取目标元素的数量
def countElements(reader) {
    int count = 0
    reader.each {
        if (it.startElement && (it.name == firstChild.name || it.name.localPart in ["inproceedings", "proceedings", "book", "incollection", "phdthesis", "mastersthesis", "www"])) {
            count++
        }
    }
    return count
}

// 创建事件读取器并计算目标元素的数量
reader = createEventReader()
elements = countElements(reader)
println "Found ${elements} target elements"

// 计算每个块包含的元素数量
chunkSize = elements / pieces
println "Splitting ${elements} <${firstChild.name.localPart}> elements into ${pieces} pieces, each with ${chunkSize} elements"

// 重新创建事件读取器
reader = createEventReader()

// 创建第一个事件写入器并添加第一个子元素
writer = createNextEventWriter()
writer.add(firstChild)

// 遍历XML事件
reader.each {
    if (it.startElement && (it.name == firstChild.name || it.name.localPart in ["inproceedings", "proceedings", "book", "incollection", "phdthesis", "mastersthesis", "www"])) {
        if (++elementCount > chunkSize) {
            writer.add(eventFactory.createEndDocument())
            writer.flush()
            writer = createNextEventWriter()
            writer.add(firstChild)
            elementCount = 0
        }
    }
    writer.add(it)
}

// 结束并刷新写入器
writer.add(eventFactory.createEndDocument())
writer.flush()
