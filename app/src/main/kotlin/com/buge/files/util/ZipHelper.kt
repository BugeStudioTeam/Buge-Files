package com.buge.files.util

import android.os.AsyncTask
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipHelper {

    interface ZipCallback {
        fun onProgress(current: Int, total: Int)
        fun onComplete(success: Boolean, message: String)
    }

    fun zipFiles(files: List<File>, outputZipFile: File, callback: ZipCallback) {
        ZipTask(files, outputZipFile, callback).execute()
    }

    fun unzipFile(zipFile: File, destinationDir: File, callback: ZipCallback) {
        UnzipTask(zipFile, destinationDir, callback).execute()
    }

    private inner class ZipTask(
        private val files: List<File>,
        private val outputZipFile: File,
        private val callback: ZipCallback
    ) : AsyncTask<Void, Int, Pair<Boolean, String>>() {

        override fun onPreExecute() {
            super.onPreExecute()
            callback.onProgress(0, files.size)
        }

        override fun doInBackground(vararg params: Void?): Pair<Boolean, String> {
            return try {
                outputZipFile.parentFile?.mkdirs()
                ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
                    var processed = 0
                    for (file in files) {
                        if (isCancelled) {
                            return Pair(false, "Cancelled")
                        }
                        addToZip(file, zos, file.name)
                        processed++
                        publishProgress(processed, files.size)
                    }
                }
                Pair(true, "Successfully compressed ${files.size} item(s)")
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, "Error: ${e.message}")
            }
        }

        private fun addToZip(file: File, zos: ZipOutputStream, name: String) {
            if (file.isDirectory) {
                val entryName = if (name.endsWith("/")) name else "$name/"
                zos.putNextEntry(ZipEntry(entryName))
                zos.closeEntry()
                file.listFiles()?.forEach { child ->
                    addToZip(child, zos, "$name/${child.name}")
                }
            } else {
                zos.putNextEntry(ZipEntry(name))
                FileInputStream(file).use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            if (values.size >= 2) {
                callback.onProgress(values[0] ?: 0, values[1] ?: 0)
            }
        }

        override fun onPostExecute(result: Pair<Boolean, String>) {
            super.onPostExecute(result)
            callback.onComplete(result.first, result.second)
        }
    }

    private inner class UnzipTask(
        private val zipFile: File,
        private val destinationDir: File,
        private val callback: ZipCallback
    ) : AsyncTask<Void, Int, Pair<Boolean, String>>() {

        private var totalEntries = 0
        private var extractedCount = 0

        override fun onPreExecute() {
            super.onPreExecute()
            try {
                ZipInputStream(FileInputStream(zipFile)).use { zis ->
                    while (zis.nextEntry != null) {
                        totalEntries++
                        zis.closeEntry()
                    }
                }
            } catch (e: Exception) {
                totalEntries = 0
            }
            callback.onProgress(0, totalEntries)
        }

        override fun doInBackground(vararg params: Void?): Pair<Boolean, String> {
            return try {
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs()
                }

                ZipInputStream(FileInputStream(zipFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (isCancelled) {
                            return Pair(false, "Cancelled")
                        }

                        val destFile = File(destinationDir, entry.name)

                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }

                        extractedCount++
                        publishProgress(extractedCount, totalEntries)

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                Pair(true, "Successfully extracted ${extractedCount} item(s)")
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, "Error: ${e.message}")
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            if (values.size >= 2) {
                callback.onProgress(values[0] ?: 0, values[1] ?: 0)
            }
        }

        override fun onPostExecute(result: Pair<Boolean, String>) {
            super.onPostExecute(result)
            callback.onComplete(result.first, result.second)
        }
    }

    fun zipFilesSync(files: List<File>, outputZipFile: File): Boolean {
        return try {
            outputZipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
                files.forEach { file ->
                    addToZipSync(file, zos, file.name)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addToZipSync(file: File, zos: ZipOutputStream, name: String) {
        if (file.isDirectory) {
            val entryName = if (name.endsWith("/")) name else "$name/"
            zos.putNextEntry(ZipEntry(entryName))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addToZipSync(child, zos, "$name/${child.name}")
            }
        } else {
            zos.putNextEntry(ZipEntry(name))
            FileInputStream(file).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    fun unzipFileSync(zipFile: File, destinationDir: File): Boolean {
        return try {
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(destinationDir, entry.name)

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}