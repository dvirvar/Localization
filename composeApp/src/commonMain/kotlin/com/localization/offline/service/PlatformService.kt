package com.localization.offline.service

import com.localization.offline.db.DatabaseAccess

class PlatformService {

    fun getAllPlatforms() = DatabaseAccess.platformDao!!.getAll()
}