package form

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.ImageIcon
import javax.swing.JButton


class PCScannerButton(text: String?, ic: ImageIcon?, backgroundColor: Color?, hoverColor: Color?, pressedColor: Color?, fill:Boolean = false) :
    JButton(text) {
    companion object {
        private const val serialVersionUID = 3514835695941372676L
    }

    init {
        this.isFocusable = false
        foreground = Color.WHITE
        background = backgroundColor
        icon = ic
        this.isBorderPainted = false
        this.isFocusPainted = false
        this.isContentAreaFilled = fill
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(evt: MouseEvent) {
                this@PCScannerButton.isFocusPainted = true
                this@PCScannerButton.isContentAreaFilled = true
                background = hoverColor
            }

            override fun mouseExited(evt: MouseEvent) {
                this@PCScannerButton.isFocusPainted = false
                this@PCScannerButton.isContentAreaFilled = fill
                background = backgroundColor
            }

            override fun mousePressed(e: MouseEvent) {
                this@PCScannerButton.isFocusPainted = true
                this@PCScannerButton.isContentAreaFilled = true
                background = pressedColor
                super.mousePressed(e)
            }
        })
    }
}
