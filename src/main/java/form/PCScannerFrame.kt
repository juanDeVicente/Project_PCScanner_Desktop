package form

import java.awt.*
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicScrollBarUI


class PCScannerFrame : JFrame(), SpawnServerThread.Listener {
    private val contentPane: JPanel
    private val thread: SpawnServerThread = SpawnServerThread(this)
    private val scrollPane: JScrollPane
    private val panelScrollView: JPanel
    private val serverList: MutableMap<Int, Process> = mutableMapOf()

    override fun mobileConnected(name: String?, androidVersion: String?, SDKVersion: String?, port: String?) {
        this.serverList[port!!.toInt()] = buildProcessExe("/binaries/app.exe", port).start()

        panelScrollView.add(MobileConnectedPanel(name))
        var panel = JPanel()
        panel.background = Color.decode("#121212")
        var flowLayout = FlowLayout()
        panel.layout = flowLayout
        var label = JLabel("Versión de Android: $androidVersion SDK: $SDKVersion")

        label.foreground = Color.WHITE
        panel.add(label)
        panelScrollView.add(panel)
        panel = JPanel()
        panel.background = Color.decode("#121212")
        flowLayout = FlowLayout()
        panel.layout = flowLayout
        label = JLabel(port)

        label.foreground = Color.WHITE
        panel.add(label)
        panelScrollView.add(panel)
        panel = JPanel()
        panel.background = Color.decode("#121212")
        flowLayout = FlowLayout()
        panel.layout = flowLayout
        val buttonDelete = PCScannerButton("Eliminar", null, Color.decode("#5500cc"), Color.decode("#4a00b3"))
        buttonDelete.addActionListener {
            this.serverList.remove(port.toInt())!!.destroy()
        }
        panel.add(buttonDelete)
        val buttonTest = PCScannerButton("Probar", null, Color.decode("#5500cc"), Color.decode("#4a00b3"))
        buttonTest.addActionListener {
            Desktop.getDesktop().browse(URI("http://localhost:$port/statics"))
        }
        panel.add(buttonTest)
        panelScrollView.add(panel)

        if (scrollPane.height < 410)
            scrollPane.setSize(scrollPane.width, scrollPane.height + 30)
        else
            scrollPane.setSize(scrollPane.width, 410)

        panelScrollView.repaint()
        revalidate()
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -4594975323325530738L

        /**
         * Launch the application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            EventQueue.invokeLater {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                    val frame = PCScannerFrame()
                    frame.isVisible = true
                } catch (e: UnsupportedLookAndFeelException) {
                } catch (e: ClassNotFoundException) {
                } catch (e: InstantiationException) {
                } catch (e: IllegalAccessException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun buildProcessExe(resourcesPath: String, vararg arguments: String): ProcessBuilder {
            val executable = javaClass.getResource(resourcesPath)
            return ProcessBuilder(executable.file.replace("%20", " "), *arguments)
        }
    }

    /**
     * Create the frame.
     */
    init {
        buildProcessExe("/binaries/OpenHardwareMonitor/OpenHardwareMonitor.exe").start()
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

        setSize(800, 450)
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
        lblTitle.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        lblTitle.foreground = Color(255, 255, 255)
        panelTitle.add(lblTitle)

        val panelButtonAction = JPanel()
        panelButtonAction.background = primaryColor
        panelButtonAction.setBounds(702, 0, 99, 24)
        panelButtonAction.layout = FlowLayout(FlowLayout.CENTER, 1, 0)
        contentPane.add(panelButtonAction)

        img = ImageIcon(javaClass.getResource("/icons/baseline_minimize_white_48dp.png")).image
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH)
        val btnMinimize: JButton = PCScannerButton("", ImageIcon(img), Color.decode("#5500cc"), Color.decode("#4a00b3"))
        btnMinimize.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        btnMinimize.addActionListener { this@PCScannerFrame.extendedState = Frame.ICONIFIED }
        panelButtonAction.add(btnMinimize)

        img = ImageIcon(javaClass.getResource("/icons/baseline_close_white_48dp.png")).image
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH)
        val btnClose: JButton = PCScannerButton("", ImageIcon(img), Color.RED, Color.decode("#ff33333"))
        btnClose.font = Font("Microsoft Sans Serif", Font.PLAIN, 12)
        btnClose.addActionListener {
            thread.close()
            Runtime.getRuntime().exec("taskkill /IM OpenHardwareMonitor.exe")
            this.removeAllServers()
            dispatchEvent(WindowEvent(this@PCScannerFrame, WindowEvent.WINDOW_CLOSING))
        }
        panelButtonAction.add(btnClose)

        val toolBar = JToolBar()
        toolBar.background = primaryColor
        toolBar.isFloatable = false
        toolBar.setBounds(0, 24, 800, 16)
        contentPane.add(toolBar)

        val btnOptions: JButton = PCScannerButton("Opciones", null, Color.decode("#5500cc"), Color.decode("#4a00b3"))
        val popupMenuOptions = JPopupMenu()
        popupMenuOptions.isOpaque = true
        popupMenuOptions.background = primaryColor
        popupMenuOptions.foreground = primaryColor
        val item = JMenuItem(object : AbstractAction("Option 1") {
            override fun actionPerformed(e: ActionEvent) {
                println("Hola que tal")
            }
        })
        item.isOpaque = true
        item.background = primaryColor
        item.foreground = Color.WHITE
        popupMenuOptions.add(item)

        btnOptions.addActionListener {
            popupMenuOptions.show(
                btnOptions,
                btnOptions.x,
                btnOptions.y + btnOptions.height
            )
        }
        toolBar.add(btnOptions)

        val btnHelp: JButton = PCScannerButton("Ayuda", null, Color.decode("#5500cc"), Color.decode("#4a00b3"))
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

        scrollPane = JScrollPane()
        scrollPane.border = null
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.setBounds(0, 40, 800, 40)
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
        lblMobileName.horizontalAlignment = SwingConstants.CENTER
        lblMobileName.foreground = Color.WHITE
        panel.add(lblMobileName)

        val lblAndroid = JLabel("Android")
        lblAndroid.horizontalAlignment = SwingConstants.CENTER
        lblAndroid.foreground = Color.WHITE
        panel.add(lblAndroid)

        val lblPortService = JLabel("Puerto del servicio")
        lblPortService.horizontalAlignment = SwingConstants.CENTER
        lblPortService.foreground = Color.WHITE
        panel.add(lblPortService)

        val lblActions = JLabel("Opciones")
        lblActions.horizontalAlignment = SwingConstants.CENTER
        lblActions.foreground = Color.WHITE
        panel.add(lblActions)

        panelScrollView = JPanel()
        panelScrollView.background = backgroundColor
        scrollPane.setViewportView(panelScrollView)
        panelScrollView.layout = GridLayout(0, 4, 0, 0)
    }

    private fun removeAllServers(){
        this.serverList.map {
            it.value.destroy()
        }
    }
}