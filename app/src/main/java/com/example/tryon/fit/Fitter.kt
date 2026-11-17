package com.example.tryon.fit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.example.tryon.data.Avatar
import com.example.tryon.data.Garment
import kotlin.math.atan2
import kotlin.math.hypot

/** Manual correction the user applies on top of the automatic anchor fit. */
data class Adjust(
    val dx: Float = 0f,        // avatar-pixel units
    val dy: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f   // degrees
)

/** One garment placed on the avatar. */
class Layer(val garment: Garment, val bitmap: Bitmap, val base: Matrix, val pivotX: Float, val pivotY: Float)

object Fitter {

    /**
     * Similarity transform (scale + rotation + translation) that maps the two garment
     * points onto the two avatar anchors. Avatars have pre-marked anchors, so no pose
     * detection model is needed and fitting is instant and consistent.
     */
    fun createLayer(avatar: Avatar, avatarBmp: Bitmap, garment: Garment, garmentBmp: Bitmap): Layer? {
        val t1 = avatar.anchors[garment.targets[0]] ?: return null
        val t2 = avatar.anchors[garment.targets[1]] ?: return null
        val aw = avatarBmp.width.toFloat()
        val ah = avatarBmp.height.toFloat()
        val gw = garmentBmp.width.toFloat()
        val gh = garmentBmp.height.toFloat()

        val tx1 = t1.x * aw; val ty1 = t1.y * ah
        val tx2 = t2.x * aw; val ty2 = t2.y * ah
        val gx1 = garment.points[0].x * gw; val gy1 = garment.points[0].y * gh
        val gx2 = garment.points[1].x * gw; val gy2 = garment.points[1].y * gh

        val gDist = hypot(gx2 - gx1, gy2 - gy1)
        val tDist = hypot(tx2 - tx1, ty2 - ty1)
        if (gDist < 1f || tDist < 1f) return null
        val s = tDist / gDist
        val rot = Math.toDegrees(
            (atan2(ty2 - ty1, tx2 - tx1) - atan2(gy2 - gy1, gx2 - gx1)).toDouble()
        ).toFloat()

        val m = Matrix().apply {
            postTranslate(-gx1, -gy1)
            postScale(s, s)
            postRotate(rot)
            postTranslate(tx1, ty1)
        }
        return Layer(garment, garmentBmp, m, (tx1 + tx2) / 2f, (ty1 + ty2) / 2f)
    }

    fun matrixFor(layer: Layer, adj: Adjust): Matrix =
        Matrix(layer.base).apply {
            postScale(adj.scale, adj.scale, layer.pivotX, layer.pivotY)
            postRotate(adj.rotation, layer.pivotX, layer.pivotY)
            postTranslate(adj.dx, adj.dy)
        }
}

object SceneRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Draws avatar + garments in avatar-pixel coordinates. Used for both preview and export. */
    fun draw(canvas: Canvas, avatar: Bitmap, layers: List<Pair<Layer, Adjust>>) {
        canvas.drawBitmap(avatar, 0f, 0f, paint)
        layers.forEach { (layer, adj) ->
            canvas.drawBitmap(layer.bitmap, Fitter.matrixFor(layer, adj), paint)
        }
    }

    fun export(avatar: Bitmap, layers: List<Pair<Layer, Adjust>>): Bitmap {
        val out = Bitmap.createBitmap(avatar.width, avatar.height, Bitmap.Config.ARGB_8888)
        draw(Canvas(out), avatar, layers)
        return out
    }
}
