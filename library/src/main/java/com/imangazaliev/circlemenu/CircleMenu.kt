package com.imangazaliev.circlemenu

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CircleMenu @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : FloatingActionButton(context, attrs) {

    val isOpened: Boolean
        get() = menuLayout.isOpened

    private val menuLayout: CircleMenuLayout

    init {
        isClickable = true

        context.obtainStyledAttributes(attrs, R.styleable.CircleMenu).apply {
            val circleStartAngle = getInteger(
                    R.styleable.CircleMenu_startAngle,
                    resources.getInteger(R.integer.circle_menu_start_angle)
            )
            val circleMaxAngle = getInteger(
                    R.styleable.CircleMenu_maxAngle,
                    resources.getInteger(R.integer.circle_menu_max_angle)
            )
            val distance = getDimension(
                    R.styleable.CircleMenu_distance,
                    resources.getDimension(R.dimen.circle_menu_distance)
            ).toInt()
            val openOnStart = getBoolean(R.styleable.CircleMenu_openOnStart, false)

            val centerButtonColorDef = ContextCompat.getColor(context, R.color.circle_menu_center_button_color)
            val centerButtonColor = getColor(R.styleable.CircleMenu_centerButtonColor, centerButtonColorDef)
            val centerButtonIconColorDef = ContextCompat.getColor(context, R.color.circle_menu_center_button_icon_color)
            val centerButtonIconColor = getColor(R.styleable.CircleMenu_centerButtonIconColor, centerButtonIconColorDef)
            val menuIconType = MenuIconType.values()[getInt(R.styleable.CircleMenu_menuIcon, 0)]

            val iconsColorDef = ContextCompat.getColor(context, R.color.circle_menu_button_icon_color)
            val buttonIconsColor = getColor(R.styleable.CircleMenu_iconsColor, iconsColorDef)
            val iconArrayId: Int = getResourceId(R.styleable.CircleMenu_buttonIcons, 0)
            val colorArrayId: Int = getResourceId(R.styleable.CircleMenu_buttonColors, 0)

            val showSelectAnimation: Boolean = getBoolean(R.styleable.CircleMenu_showSelectAnimation, true)

            val colors = resources.getIntArray(colorArrayId).asList()
            val icons = resources.obtainTypedArray(iconArrayId).let { iconsIds ->
                (0 until iconsIds.length()).map {
                    iconsIds.getResourceId(it, -1)
                }
            }

            if (colors.isEmpty() || icons.isEmpty()) {
                throw IllegalArgumentException("Colors and icons array must not be empty")
            }

            if (colors.size != icons.size) {
                throw IllegalArgumentException("Colors array size must be equal to the icons array")
            }

            menuLayout = CircleMenuLayout(
                    context = context,
                    centerButtonColor = centerButtonColor,
                    centerButtonIconColor = centerButtonIconColor,
                    menuIconType = menuIconType,
                    buttonIconsColor = buttonIconsColor,
                    distance = distance,
                    circleMaxAngle = circleMaxAngle,
                    circleStartAngle = circleStartAngle,
                    showSelectAnimation = showSelectAnimation,
                    openOnStart = openOnStart,
                    colors = colors,
                    icons = icons
            )

            initMenuButton(menuIconType, centerButtonColor, centerButtonIconColor)
            initMenuLayout()
            // Asegura que el botón lanzador no duplique visualmente al centro del menú
            onMenuCloseAnimationEnd {
                this@CircleMenu.visibility = View.VISIBLE
            }
        }.recycle()
    }

    private fun initMenuButton(
            iconType: MenuIconType,
            buttonColor: Int,
            iconColor: Int
    ) {
        val iconResId = if (iconType == MenuIconType.PLUS) {
            R.drawable.ic_plus
        } else {
            R.drawable.ic_menu
        }
        val icon = ContextCompat.getDrawable(context, iconResId)!!
        icon.setTintCompat(iconColor)
        setImageDrawable(icon)
        backgroundTintList = ColorStateList.valueOf(buttonColor)
    }

    private fun initMenuLayout() {
        val menuButton = this
        // Resolve a container ViewGroup to host the menu layout.
        // Prefer the window root view; fallback to the direct parent.
        val containerResolver: () -> ViewGroup? = {
            // Si rootView no es un ViewGroup, usa el padre directo.
            (menuButton.rootView as? ViewGroup)
                ?: (menuButton.parent as? ViewGroup)
        }

        onLaidOut {
            val menuButtonLocation = IntArray(2).let {
                menuButton.getLocationOnScreen(it)
                Point(it.first(), it.last())
            }

            val bounds = Rect(menuButtonLocation.x, menuButtonLocation.y,
                    menuButtonLocation.x + menuButton.width,
                    menuButtonLocation.y + menuButton.height
            )

            val container = containerResolver() ?: return@onLaidOut
            val parentLocation = IntArray(2).let {
                container.getLocationOnScreen(it)
                Point(it.first(), it.last())
            }
            bounds.offset(-parentLocation.x, -parentLocation.y)

            // Asegura que el contenedor no recorte (si es posible)
            container.clipChildren = false

            // Añade solo si aún no tiene padre o si es otro contenedor
            if (menuLayout.parent != container) {
                (menuLayout.parent as? ViewGroup)?.removeView(menuLayout)
                val menuLayoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                container.addView(menuLayout, menuLayoutParams)
            }

            menuLayout.post {
                val width = menuLayout.width
                val height = menuLayout.height
                val params = (menuLayout.layoutParams as? FrameLayout.LayoutParams)
                    ?: FrameLayout.LayoutParams(width, height)
                params.width = width
                params.height = height
                params.leftMargin = bounds.centerX() - (menuLayout.width / 2)
                params.topMargin = bounds.centerY() - (menuLayout.height / 2)
                menuLayout.layoutParams = params
            }
        }

        // Reposiciona el layout del menú cuando cambie la posición del botón (p.ej. arrastre)
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val container = (rootView as? ViewGroup) ?: (parent as? ViewGroup) ?: return@addOnLayoutChangeListener
            container.clipChildren = false
            val btnLoc = IntArray(2).also { getLocationOnScreen(it) }
            val parentLoc = IntArray(2).also { container.getLocationOnScreen(it) }
            val bounds = Rect(
                btnLoc[0],
                btnLoc[1],
                btnLoc[0] + width,
                btnLoc[1] + height
            )
            bounds.offset(-parentLoc[0], -parentLoc[1])

            // Asegura que el menú esté añadido al contenedor
            if (menuLayout.parent != container) {
                (menuLayout.parent as? ViewGroup)?.removeView(menuLayout)
                container.addView(menuLayout, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ))
            }

            if (menuLayout.width == 0 || menuLayout.height == 0) {
                menuLayout.post { requestLayout() }
            } else {
                val lp = (menuLayout.layoutParams as? FrameLayout.LayoutParams)
                    ?: FrameLayout.LayoutParams(menuLayout.width, menuLayout.height)
                lp.width = menuLayout.width
                lp.height = menuLayout.height
                lp.leftMargin = bounds.centerX() - (menuLayout.width / 2)
                lp.topMargin = bounds.centerY() - (menuLayout.height / 2)
                menuLayout.layoutParams = lp
            }
        }



        super.setOnClickListener {
            showMenu()
        }
    }

    private fun showMenu() {
        // Oculta el botón lanzador para no ver dos botones al mismo tiempo
        this.visibility = View.INVISIBLE
        menuLayout.visibility = View.VISIBLE
        menuLayout.open(true)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        //empty
    }

    fun setOnItemClickListener(listener: (buttonIndex: Int) -> Unit) {
        this.menuLayout.setOnItemClickListener(listener)
    }

    fun setOnItemLongClickListener(listener: (buttonIndex: Int) -> Unit) {
        this.menuLayout.setOnItemLongClickListener(listener)
    }

    fun toggle() {
        this.menuLayout.toggle()
    }

    fun open(animate: Boolean = true) {
        this.menuLayout.open(animate)
    }

    fun close(animate: Boolean) {
        this.menuLayout.close(animate)
    }

    // Expuesto por si se requiere desde fuera (no usado ahora)
    fun repositionMenuRelativeToButton() {
        val container = (rootView as? ViewGroup) ?: (parent as? ViewGroup) ?: return
        container.clipChildren = false
        val btnLoc = IntArray(2).also { getLocationOnScreen(it) }
        val parentLoc = IntArray(2).also { container.getLocationOnScreen(it) }
        val bounds = Rect(
            btnLoc[0],
            btnLoc[1],
            btnLoc[0] + width,
            btnLoc[1] + height
        )
        bounds.offset(-parentLoc[0], -parentLoc[1])
        val lp = (menuLayout.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(menuLayout.width, menuLayout.height)
        lp.leftMargin = bounds.centerX() - (menuLayout.width / 2)
        lp.topMargin = bounds.centerY() - (menuLayout.height / 2)
        menuLayout.layoutParams = lp
    }

    fun onMenuOpenAnimationStart(listener: () -> Unit) {
        this.menuLayout.onMenuOpenAnimationStart(listener)
    }

    fun onMenuOpenAnimationEnd(listener: () -> Unit) {
        this.menuLayout.onMenuOpenAnimationEnd(listener)
    }

    fun onMenuCloseAnimationStart(listener: () -> Unit) {
        this.menuLayout.onMenuCloseAnimationStart(listener)
    }

    fun onMenuCloseAnimationEnd(listener: () -> Unit) {
        this.menuLayout.onMenuCloseAnimationEnd(listener)
    }

    fun onButtonClickAnimationStart(listener: (buttonIndex: Int) -> Unit) {
        this.menuLayout.onButtonClickAnimationStart(listener)
    }

    fun onButtonClickAnimationEnd(listener: (buttonIndex: Int) -> Unit) {
        this.menuLayout.onButtonClickAnimationEnd(listener)
    }

    internal enum class MenuIconType(val code: String) {
        HAMBURGER("hamburger"), PLUS("plus")
    }

}
