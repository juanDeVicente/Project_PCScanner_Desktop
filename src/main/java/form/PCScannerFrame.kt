package form

import org.json.JSONArray
import org.json.JSONObject
import java.awt.*
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicScrollBarUI


class PCScannerFrame : JFrame(), SpawnServerThread.Listener {
    private val contentPane: JPanel
    private val scrollPane: JScrollPane
    private val panelScrollView: JPanel
    private val serverList: MutableMap<String, Process> = mutableMapOf()
    private val ipPortMap: MutableMap<String, MutableList<String>> = mutableMapOf() // Esta es la lista que tengo que guardar para mantener los servidores

    private val thread: SpawnServerThread = SpawnServerThread(this, mutableListOf())

    private lateinit var trayIcon: TrayIcon
    private lateinit var systemTray: SystemTray

    private var startMinimized = true

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -4594975323325530738L
        private val OS: String = System.getProperty("os.name").toLowerCase()
        /**
         * Launch the application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            EventQueue.invokeLater {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                    PCScannerFrame()
                } catch (e: UnsupportedLookAndFeelException) {
                } catch (e: ClassNotFoundException) {
                } catch (e: InstantiationException) {
                } catch (e: IllegalAccessException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun buildProcessExe(filename: String, vararg arguments: String): ProcessBuilder {
            return ProcessBuilder(File(filename).absolutePath, *arguments)
        }

        fun isWindows(): Boolean {
            return OS.indexOf("win") >= 0
        }
    }

    /**
     * Create the frame.
     */
    init {
        thread.start()

        val primaryColor = Color.decode("#6200EE")
        val backgroundColor = Color.decode("#121212")
        var img = ImageIcon(javaClass.getResource("/icons/logo_icon.png")).image
            .getScaledInstance(64, 64, Image.SCALE_SMOOTH)
        iconImage = img
        isUndecorated = true
        isResizable = false
        isLocationByPlatform = true
        title = "Project PCScanner Desktop"
        defaultCloseOperation = EXIT_ON_CLOSE

        setSize(800, 440)
        setLocationRelativeTo(null)

        contentPane = JPanel()
        contentPane.background = backgroundColor
        contentPane.border = EmptyBorder(5, 5, 5, 5)
        setContentPane(contentPane)
        contentPane.layout = null

        val panelTitle: JPanel = MotionPanel(this)
        panelTitle.background = primaryColor
        panelTitle.setBounds(0, 0, 702, 24)
        contentPane.add(panelTitle)
        panelTitle.layout = FlowLayout(FlowLayout.LEFT, 5, 5)

        val lblIcon = JLabel("")
        img = ImageIcon(javaClass.getResource("/icons/logo_icon_window.png")).image
            .getScaledInstance(14, 14, Image.SCALE_SMOOTH)
        lblIcon.icon = ImageIcon(img)
        lblIcon.horizontalAlignment = SwingConstants.CENTER
        panelTitle.add(lblIcon)

        val lblTitle = JLabel("Project PCScanner - Desktop")
        //lblTitle.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        lblTitle.foreground = Color(255, 255, 255)
        panelTitle.add(lblTitle)

        val panelButtonAction = JPanel()
        panelButtonAction.background = primaryColor
        panelButtonAction.setBounds(702, 0, 99, 24)
        panelButtonAction.layout = FlowLayout(FlowLayout.CENTER, 1, 0)
        contentPane.add(panelButtonAction)

        img = ImageIcon(javaClass.getResource("/icons/baseline_minimize_white_48dp.png")).image
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH)
        val btnMinimize: JButton = PCScannerButton(
            "",
            ImageIcon(img),
            primaryColor,
            Color.decode("#5500cc"),
            Color.decode("#4a00b3")
        )
        //btnMinimize.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        btnMinimize.addActionListener {
            this@PCScannerFrame.extendedState = Frame.ICONIFIED
        }
        panelButtonAction.add(btnMinimize)

        img = ImageIcon(javaClass.getResource("/icons/baseline_close_white_48dp.png")).image
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH)
        val btnClose: JButton =
            PCScannerButton("", ImageIcon(img), Color.RED, Color.RED, Color.decode("#ff33333"))
        //btnClose.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        btnClose.addActionListener {
            this@PCScannerFrame.dispatchEvent(WindowEvent(this@PCScannerFrame, WindowEvent.WINDOW_CLOSING))
        }
        panelButtonAction.add(btnClose)

        scrollPane = JScrollPane()
        scrollPane.border = null
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.setBounds(0, 40, 800, 400)
        contentPane.add(scrollPane)
        scrollPane.verticalScrollBar.background = primaryColor

        scrollPane.verticalScrollBar.setUI(object : BasicScrollBarUI() {
            override fun configureScrollBarColors() {
                thumbColor = Color.WHITE
                trackColor = primaryColor
            }

            override fun createDecreaseButton(orientation: Int): JButton {
                val button = super.createDecreaseButton(orientation)
                button.background = primaryColor
                button.foreground = Color.WHITE
                return button
            }

            override fun createIncreaseButton(orientation: Int): JButton {
                val button = super.createIncreaseButton(orientation)
                button.background = primaryColor
                button.foreground = Color.WHITE
                return button
            }
        })

        val panel = JPanel()
        panel.border = null
        panel.background = backgroundColor
        scrollPane.setColumnHeaderView(panel)
        panel.layout = GridLayout(0, 4, 5, 5)
        val lblMobileName = JLabel("Nombre del m\u00F3vil")
        val margin: Border = EmptyBorder(1, 4, 0, 0)
        lblMobileName.border = margin
        lblMobileName.horizontalAlignment = SwingConstants.LEFT
        lblMobileName.foreground = Color.WHITE
        panel.add(lblMobileName)

        val lblAndroid = JLabel("Android")
        lblAndroid.border = margin
        lblAndroid.horizontalAlignment = SwingConstants.LEFT
        lblAndroid.foreground = Color.WHITE
        panel.add(lblAndroid)

        val lblPortService = JLabel("Puerto del servicio")
        lblPortService.border = margin
        lblPortService.horizontalAlignment = SwingConstants.LEFT
        lblPortService.foreground = Color.WHITE
        panel.add(lblPortService)

        val lblActions = JLabel("Opciones")
        lblActions.border = margin
        lblActions.horizontalAlignment = SwingConstants.LEFT
        lblActions.foreground = Color.WHITE
        panel.add(lblActions)

        panelScrollView = JPanel()
        panelScrollView.background = backgroundColor
        scrollPane.setViewportView(panelScrollView)
        panelScrollView.layout = GridLayout(0, 4, 0, 0)

        for (i in 0 until 12 * 4) {
            val p = JPanel()
            p.background = backgroundColor
            panelScrollView.add(p)
        }

        this.readAndApplyConfig()

        val toolBar = JToolBar()
        toolBar.background = primaryColor
        toolBar.isFloatable = false
        toolBar.setBounds(0, 24, 800, 16)
        contentPane.add(toolBar)

        val btnOptions: JButton = PCScannerButton(
            "Opciones",
            null,
            primaryColor,
            Color.decode("#5500cc"),
            Color.decode("#4a00b3")
        )
        val popupMenuOptions = JPopupMenu()
        popupMenuOptions.isOpaque = true
        popupMenuOptions.background = primaryColor
        popupMenuOptions.foreground = primaryColor


        val itemLimitPort = JMenuItem(object : AbstractAction("Excluir puertos") {
            override fun actionPerformed(e: ActionEvent) {
                var input: String?
                val regex = Regex("^[0-9]*$")
                do {
                    input = JOptionPane.showInputDialog(
                        this@PCScannerFrame,
                        "Introduce el puerto a excluir.\nPuertos ya excluidos: ${this@PCScannerFrame.thread.excludedPorts}",
                        "Añadir puerto",
                        JOptionPane.QUESTION_MESSAGE
                    )
                    if (input == null) //El user ha pulsado cancelar
                        break
                    input.trim()

                    if (input == "" || !input.matches(regex))
                    {
                        if (isWindows())
                            doWindowsSound()
                        JOptionPane.showMessageDialog(
                            this@PCScannerFrame,
                            "Parece que el puerto introducido no es válido",
                            "Error de puerto",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    else if (this@PCScannerFrame.thread.excludedPorts.contains(input))
                    {
                        if (isWindows())
                            doWindowsSound()
                        JOptionPane.showMessageDialog(
                            this@PCScannerFrame,
                            "Parece que el puerto introducido ya ha sido añadido",
                            "Error de puerto",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    else
                        this@PCScannerFrame.thread.excludedPorts.add(input)
                } while (input == "" || !input!!.matches(regex)) //Lo de que no vuelva a saltar el dialog cuando se introduce un puerto ya introducido es adrede
            }
        })
        itemLimitPort.isOpaque = true
        itemLimitPort.background = primaryColor
        itemLimitPort.foreground = Color.WHITE
        popupMenuOptions.add(itemLimitPort)

        val itemDeleteLimitPort = JMenuItem(object : AbstractAction("Eliminar limitación de puerto") {
            override fun actionPerformed(e: ActionEvent) {
                val port = JOptionPane.showInputDialog(
                    this@PCScannerFrame,
                    "Escoge el puerto a desexcluir",
                    "Desexcluir puerto",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    this@PCScannerFrame.thread.excludedPorts.toTypedArray(),
                    this@PCScannerFrame.thread.excludedPorts[0]
                )
                if (port != null)
                    this@PCScannerFrame.thread.excludedPorts.remove(port)
            }
        })
        itemDeleteLimitPort.isOpaque = true
        itemDeleteLimitPort.background = primaryColor
        itemDeleteLimitPort.foreground = Color.WHITE
        popupMenuOptions.add(itemDeleteLimitPort)

        val itemDeleteServers = JMenuItem(object : AbstractAction("Eliminar todos los servidores") {
            override fun actionPerformed(e: ActionEvent) {
                if (this@PCScannerFrame.serverList.isNotEmpty()) {
                    this@PCScannerFrame.removeAllServers()
                    this@PCScannerFrame.panelScrollView.removeAll()
                    for (i in 0 until 4 * 12) {
                        val p = JPanel()
                        p.background = backgroundColor
                        this@PCScannerFrame.panelScrollView.add(p)
                    }
                    this@PCScannerFrame.panelScrollView.repaint()
                    this@PCScannerFrame.revalidate()
                    if (isWindows())
                        doWindowsSound()
                    JOptionPane.showMessageDialog(
                        this@PCScannerFrame,
                        "¡Los servidores han sido borrados correctamente!"
                    )
                }
                else {
                    if (isWindows())
                        doWindowsSound()
                    JOptionPane.showMessageDialog(
                        this@PCScannerFrame,
                        "Parece que no hay servidores lanzados",
                        "No hay servidores",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        })
        itemDeleteServers.isOpaque = true
        itemDeleteServers.background = primaryColor
        itemDeleteServers.foreground = Color.WHITE
        popupMenuOptions.add(itemDeleteServers)

        val itemDeleteOutputs = JMenuItem(object : AbstractAction("Eliminar todos los output de los servers") {
            override fun actionPerformed(e: ActionEvent) {
                val directory = File("serverOutput")
                if (Files.exists(directory.toPath()))
                    for (file in directory.listFiles()!!) //No se espera tener recursividad asi que no hace falta eliminar los subdirecttorios
                        if (!file.isDirectory)
                            file.delete()
                if (isWindows())
                    doWindowsSound()
                JOptionPane.showMessageDialog(
                    this@PCScannerFrame,
                    "¡Todos los output han sido borrados correctamente!"
                )
            }
        })
        itemDeleteOutputs.isOpaque = true
        itemDeleteOutputs.background = primaryColor
        itemDeleteOutputs.foreground = Color.WHITE
        popupMenuOptions.add(itemDeleteOutputs)

        btnOptions.addActionListener {
            popupMenuOptions.show(
                btnOptions,
                btnOptions.x,
                btnOptions.y + btnOptions.height
            )
        }

        val itemStartMinimized = object: JCheckBoxMenuItem("Lanzar la aplicación minimizada") {
            override fun processMouseEvent(evt: MouseEvent) {
                if (evt.id == MouseEvent.MOUSE_RELEASED && contains(evt.point)) {
                    doClick()
                    isArmed = true
                } else {
                    super.processMouseEvent(evt)
                }
            }
        }
        itemStartMinimized.isOpaque = true
        itemStartMinimized.background = primaryColor
        itemStartMinimized.foreground = Color.WHITE
        itemStartMinimized.addItemListener {
            this.startMinimized = itemStartMinimized.isSelected
        }

        itemStartMinimized.isSelected = this.startMinimized
        popupMenuOptions.add(itemStartMinimized)

        toolBar.add(btnOptions)

        val btnHelp: JButton = PCScannerButton(
            "Ayuda",
            null,
            primaryColor,
            Color.decode("#5500cc"),
            Color.decode("#4a00b3")
        )
        btnHelp.addActionListener {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(URI("https://github.com/juanDeVicente/Project_PC_Scanner"))
                } catch (e1: IOException) {
                    e1.printStackTrace()
                } catch (e1: URISyntaxException) {
                    e1.printStackTrace()
                }
            } else JOptionPane.showMessageDialog(
                this@PCScannerFrame,
                "Parece que no es posible abrir el navegador de internet.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
        toolBar.add(btnHelp)

        if (SystemTray.isSupported())
        {
            systemTray = SystemTray.getSystemTray() //Tengo que comprobar que el system tray esta habilitado antes de asignarlo
            img = Toolkit.getDefaultToolkit().getImage(javaClass.getResource("/icons/logo_icon.png"))
            val popup = PopupMenu()
            var item = MenuItem("Eliminar todos los servidores")
            item.addActionListener {
                if (this@PCScannerFrame.serverList.isNotEmpty()) {
                    this@PCScannerFrame.removeAllServers()
                    this@PCScannerFrame.panelScrollView.removeAll()
                    for (i in 0 until 4 * 12) {
                        val p = JPanel()
                        p.background = backgroundColor
                        this@PCScannerFrame.panelScrollView.add(p)
                    }
                    this@PCScannerFrame.panelScrollView.repaint()
                    this@PCScannerFrame.revalidate()
                    if (isWindows())
                        doWindowsSound()
                    JOptionPane.showMessageDialog(
                        this@PCScannerFrame,
                        "¡Los servidores han sido borrados correctamente!"
                    )
                }
                else {
                    if (isWindows())
                        doWindowsSound()
                    JOptionPane.showMessageDialog(
                        this@PCScannerFrame,
                        "Parece que no hay servidores lanzados",
                        "No hay servidores",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
            popup.add(item)

            item = MenuItem("Eliminar todos los outputs de los servidores")
            item.addActionListener {
                val directory = File("serverOutput")
                if (Files.exists(directory.toPath()))
                    for (file in directory.listFiles()!!) //No se espera tener recursividad asi que no hace falta eliminar los subdirectorios
                        if (!file.isDirectory)
                            file.delete()
                if (isWindows())
                    doWindowsSound()
                JOptionPane.showMessageDialog(
                    this@PCScannerFrame,
                    "¡Todos los output han sido borrados correctamente!"
                )
            }
            popup.add(item)

            popup.addSeparator()

            item = MenuItem("Cerrar")
            item.addActionListener {
                this@PCScannerFrame.dispatchEvent(WindowEvent(this@PCScannerFrame, WindowEvent.WINDOW_CLOSING))
            }
            popup.add(item)

            item = MenuItem("Abrir")
            item.addActionListener {
                this@PCScannerFrame.isVisible = true
                this@PCScannerFrame.extendedState = NORMAL
            }
            popup.add(item)

            trayIcon = TrayIcon(img, "Project PCScanner Desktop", popup)
            trayIcon.isImageAutoSize = true
            trayIcon.addActionListener {
                this@PCScannerFrame.isVisible = true
                this@PCScannerFrame.extendedState = NORMAL
            }

            this.addWindowStateListener {
                if (it.newState == Frame.ICONIFIED || it.newState == 7)
                {
                    systemTray.add(trayIcon)
                    this@PCScannerFrame.isVisible = false
                }
                else if (it.newState == Frame.MAXIMIZED_BOTH || it.newState == Frame.NORMAL)
                {
                    systemTray.remove(trayIcon)
                    this@PCScannerFrame.isVisible = true
                }
            }
            if (this.startMinimized)
            {
                systemTray.add(trayIcon)
                this.isVisible = false
                this.extendedState = Frame.ICONIFIED
            }
            else
                this.isVisible = true
        }
        this.addWindowListener(object: WindowAdapter(){
            override fun windowClosing(e: WindowEvent?) {
                this@PCScannerFrame.closeApp()
            }
        })
    }

    override fun mobileConnected(name: String?, androidVersion: String?, SDKVersion: String?, ip: String?, port: String?): String {

        if (serverList.containsKey(ip!!))
            return ipPortMap[ip]!![0]
        this.serverList[ip] = buildProcessExe(
            "binaries/Project PCScanner Server/Project PCScanner Server.exe",
            port!!
        ).start()
        this.ipPortMap[ip] = mutableListOf()

        this.ipPortMap[ip]!!.add(port)
        this.ipPortMap[ip]!!.add(name!!)
        this.ipPortMap[ip]!!.add(androidVersion!!)
        this.ipPortMap[ip]!!.add(SDKVersion!!)
        val p0 = panelScrollView.add(MobileConnectedPanel(name), 4 * (serverList.size - 1))

        var panel = JPanel()
        panel.background = Color.decode("#121212")
        var flowLayout = FlowLayout(FlowLayout.LEFT)
        panel.layout = flowLayout
        var label = JLabel("Versión de Android: $androidVersion SDK: $SDKVersion")
        label.foreground = Color.WHITE
        panel.add(label)
        val p1 = panelScrollView.add(panel, (4 * (serverList.size - 1)) + 1)

        panel = JPanel()
        panel.background = Color.decode("#121212")
        flowLayout = FlowLayout(FlowLayout.LEFT)
        panel.layout = flowLayout
        label = JLabel(port)
        label.foreground = Color.WHITE
        panel.add(label)
        val p2 = panelScrollView.add(panel, (4 * (serverList.size - 1)) + 2)

        panel = JPanel()
        panel.background = Color.decode("#121212")
        flowLayout = FlowLayout(FlowLayout.LEFT)
        panel.layout = flowLayout
        val buttonDelete = PCScannerButton(
            "Eliminar",
            null,
            Color.decode("#6200EE"),
            Color.decode("#5500cc"),
            Color.decode("#4a00b3"),
            true
        )
        panel.add(buttonDelete)

        val buttonTest = PCScannerButton(
            "Probar",
            null,
            Color.decode("#6200EE"),
            Color.decode("#5500cc"),
            Color.decode("#4a00b3"),
            true
        )
        buttonTest.addActionListener {
            Desktop.getDesktop().browse(URI("http://localhost:$port/statics"))
        }
        panel.add(buttonTest)
        val p3 = panelScrollView.add(panel, (4 * (serverList.size - 1)) + 3)

        buttonDelete.addActionListener {
            destroyServerWindows(this.serverList.remove(ip)!!.pid().toString())
            this.ipPortMap.remove(ip)

            panelScrollView.remove(p0)
            panelScrollView.remove(p1)
            panelScrollView.remove(p2)
            panelScrollView.remove(p3)
            panelScrollView.repaint()

            if (serverList.size < 12 ) {
                for (i in 0 until 4)
                {
                    val p = JPanel()
                    p.background = Color.decode("#121212")
                    panelScrollView.add(p, (this.serverList.size * 4) + i)
                }
            }
            panelScrollView.repaint()
            revalidate()
        }

        if (serverList.size <= 12 ) {
            panelScrollView.remove(11 * 4)
            panelScrollView.remove((11 * 4) + 1)
            panelScrollView.remove((11 * 4) + 2)
            panelScrollView.remove((11 * 4) + 3)
        }

        panelScrollView.repaint()
        revalidate()

        return port
    }

    private fun removeAllServers(){
        val sdf = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss")
        val currentDate = sdf.format(Date())
        this.serverList.map {
            destroyServerWindows(it.value.pid().toString())
            val fw = FileWriter("serverOutput/${currentDate}_${it.key}.txt")
            fw.write(String(it.value.inputStream.readAllBytes()))
            fw.close()
        }
        this.serverList.clear()
    }
    private fun destroyServerWindows(pid: String) {
        Runtime.getRuntime().exec("TASKKILL /PID $pid /T /F")
    }


    private fun doWindowsSound() {
        val runnable = Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation") as Runnable
        runnable.run()
    }

    private fun closeApp() {
        thread.close()
        this.removeAllServers()
        writeConfig()
    }

    private fun writeConfig() {
        val obj = JSONObject()
        obj.put("startMinimized", this.startMinimized)
        val serversArray = JSONArray()
        for (item in ipPortMap)
        {
            val serverObj = JSONObject()
            serverObj.put("ip", item.key)
            serverObj.put("port", item.value[0])
            serverObj.put("name", item.value[1])
            serverObj.put("version", item.value[2])
            serverObj.put("sdkVersion", item.value[3])
            serversArray.put(serverObj)
        }
        obj.put("servers", serversArray)
        val excludedPorts = JSONArray(this.thread.excludedPorts)
        obj.put("excludedPorts", excludedPorts)

        val f = File("./config.json")
        if (!f.exists())
            f.createNewFile()
        val fw = FileWriter(f)

        try {
            fw.write(obj.toString(2))
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        finally {
            try {
                fw.flush()
                fw.close()
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun readAndApplyConfig() {
        val f = File("./config.json")
        if (!f.exists())
            return
        try {
            FileReader(f).use { reader ->
                val obj = JSONObject(reader.readText())
                this.startMinimized = obj.get("startMinimized") as Boolean
                for (item in obj.getJSONArray("servers")) {
                    val serverObj = item as JSONObject
                    this.mobileConnected(serverObj.getString("name"), serverObj.getString("version"), serverObj.getString("sdkVersion"), serverObj.getString("ip"), serverObj.getString("port"))
                }
                for (item in obj.getJSONArray("excludedPorts")) {
                    val port = item as String
                    this.thread.excludedPorts.add(port)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}