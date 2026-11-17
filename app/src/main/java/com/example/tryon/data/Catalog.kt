package com.example.tryon.data

import android.content.Context
import org.json.JSONObject

data class Pt(val x: Float, val y: Float)

data class Avatar(
    val id: String,
    val name: String,
    val file: String,
    /** Named body anchors, normalized 0..1 relative to the avatar image. */
    val anchors: Map<String, Pt>
)

data class Garment(
    val id: String,
    val name: String,
    val file: String,
    /** "top" or "bottom" - one garment per category is shown at a time. */
    val category: String,
    /** Two avatar anchor names this garment is pinned to. */
    val targets: List<String>,
    /** Two matching points on the garment image, normalized 0..1. */
    val points: List<Pt>
)

data class Catalog(val avatars: List<Avatar>, val garments: List<Garment>)

object CatalogRepository {
    fun load(context: Context): Catalog {
        val text = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)

        val avatars = root.getJSONArray("avatars").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val a = o.getJSONObject("anchors")
                val anchors = a.keys().asSequence().associateWith { k ->
                    val p = a.getJSONArray(k)
                    Pt(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
                }
                Avatar(o.getString("id"), o.getString("name"), o.getString("file"), anchors)
            }
        }

        val garments = root.getJSONArray("garments").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val t = o.getJSONArray("targets")
                val p = o.getJSONArray("points")
                Garment(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    file = o.getString("file"),
                    category = o.getString("category"),
                    targets = listOf(t.getString(0), t.getString(1)),
                    points = (0 until 2).map {
                        val pp = p.getJSONArray(it)
                        Pt(pp.getDouble(0).toFloat(), pp.getDouble(1).toFloat())
                    }
                )
            }
        }
        return Catalog(avatars, garments)
    }
}
