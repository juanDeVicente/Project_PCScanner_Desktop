package form

import java.awt.Color
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class MobileConnectedPanel(mobileName: String?) : JPanel() {
    companion object {
        private const val serialVersionUID = 6179306375667804628L
    }

    /**
     * Create the panel.
     */
    init {
        background = Color.decode("#121212")
        var image: Image? = null
        val lblMobileImage = JLabel("")
        lblMobileImage.horizontalAlignment = SwingConstants.CENTER
        image = ImageIcon(MobileConnectedPanel::class.java.getResource("/icons/mobile_icon.png")).image
            .getScaledInstance(11, 17, Image.SCALE_SMOOTH)
        lblMobileImage.icon = ImageIcon(image)
        add(lblMobileImage)
        val lblMobileName = JLabel(mobileName)
        lblMobileName.horizontalAlignment = SwingConstants.LEFT
        lblMobileName.foreground = Color.WHITE
        add(lblMobileName)
    }
}
