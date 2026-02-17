package cc.tomko.outify.ui.repository

import androidx.room.withTransaction
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.database.impl.LikedTrackEntity

class LikedRepository(
    private val db: AppDatabase,
    private val likedDao: LikedDao,
) {
    suspend fun addLikedTrack(uri: String, position: Double, addedAt: Long = System.currentTimeMillis()) {
//        db.withTransaction {
//            likedDao.shiftPositions(position)
//            likedDao.insert(
//                LikedTrackEntity(
//                    trackUri = uri,
//                    position = position,
//                    addedAt = addedAt
//                )
//            )
//        }
    }

    suspend fun removeTrack(uri: String, fromPosition: Double) {
//        db.withTransaction {
//            likedDao.delete(uri)
//            likedDao.shiftPositionsDown(fromPosition)
//        }
    }
}