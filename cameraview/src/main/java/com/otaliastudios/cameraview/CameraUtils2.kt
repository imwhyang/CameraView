@file:JvmName("CameraUtils2")

package com.otaliastudios.cameraview

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.SizeF
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.otaliastudios.cameraview.controls.Facing
import java.util.Arrays
import java.util.Comparator

/**
 *@Author : wenhaiyang
 *@Date : 2024/8/21
 *@Name : CameraUtils2
 *@Desc:
 */
open class CameraUtils2 {
    companion object {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @JvmName("getCameraLensInfo")
        fun getCameraLensInfo(cameraManager: CameraManager, facing: Int): CameraInformation? {
//            val cameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
//                ?: return null
            val cameraIdList = cameraManager.cameraIdList
            val stringBuilder = StringBuilder()
            stringBuilder.appendln("device: model = ${Build.MODEL}, board = ${Build.BOARD}, device = ${Build.DEVICE}, manufacturer = ${Build.MANUFACTURER}, product = ${Build.PRODUCT}\n")
            stringBuilder.appendln("camera1: " + Camera.getNumberOfCameras())
            stringBuilder.appendln("camera2: " + Arrays.toString(cameraIdList))
            stringBuilder.appendln()

            val frontCameraList = arrayListOf<CameraInformation>()
            val backCameraList = arrayListOf<CameraInformation>()
            var physicalCameraIds:Set<String> = setOf()
            cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                // Usually cameraId = 0 is logical camera, so we check that
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )
                val isLogicalCamera = capabilities!!.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                )

                if (isLogicalCamera) {
                    physicalCameraIds = characteristics.physicalCameraIds
                }
                //前置
                val front =
                    characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                stringBuilder.appendln("camera id = $id: front = $front")
                //获取相机的物理尺寸
                val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                stringBuilder.appendln("sensor info : width =  ${size?.width}, height = ${size?.height}")

                val focalLens1 =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!
                stringBuilder.appendln("focal lens = ${focalLens1.contentToString()}")
                val w = size!!.width
                val h = size.height
                val horizontalAngle = (2 * Math.atan(w / (focalLens1[0] * 2).toDouble())).toFloat()
                val verticalAngle = (2 * Math.atan(h / (focalLens1[0] * 2).toDouble())).toFloat()
                stringBuilder.appendln("horizontalAngle = $horizontalAngle")
                stringBuilder.appendln("verticalAngle = $verticalAngle")

                stringBuilder.appendln()
                val cameraInfo = CameraInformation(
                    cameraId = id,
                    isFrontFacing = front,
                    sensorSize = size,
                    fovHorizontal = horizontalAngle,
                    fovVertical = verticalAngle
                )
                if (front) {
                    frontCameraList.add(cameraInfo)
                } else if (!isLogicalCamera){
                    backCameraList.add(cameraInfo)
                }
            }

            //简单判断哪个是广角镜头
            //通过FOV(field of view)排序, 从小到大, 角度越大, 越是广角
            val comparator = object : Comparator<CameraInformation> {
                override fun compare(o1: CameraInformation, o2: CameraInformation): Int {
                    val o1FovSize = o1.fovHorizontal * o1.fovVertical
                    val o2FovSize = o2.fovHorizontal * o2.fovVertical
                    if (o1FovSize > o2FovSize) {
                        return 1
                    } else if (o1FovSize < o2FovSize) {
                        return -1
                    }
                    return 0
                }
            }

            frontCameraList.sortWith(comparator)
            backCameraList.sortWith(comparator)

            stringBuilder.appendln("front camera result:")
            frontCameraList.forEach {
                stringBuilder.appendln("id = ${it.cameraId}, size = ${it.sensorSize} fovH = ${it.fovHorizontal}")
            }
            stringBuilder.appendln("front wide camera is ${frontCameraList.last()}")
            stringBuilder.appendln()

            stringBuilder.appendln("back camera result:")
            backCameraList.forEach {
                stringBuilder.appendln("id = ${it.cameraId}, size = ${it.sensorSize} fovH = ${it.fovHorizontal}")
            }
            stringBuilder.appendln("back wide camera is ${backCameraList.last()}")


//        textView.text = stringBuilder
//        return stringBuilder.toString()
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return frontCameraList.last()
            } else {
//                return backCameraList.find {
//                    physicalCameraIds.last() == it.cameraId
//                }
                return backCameraList.last()
            }
        }
    }
}

data class CameraInformation(
    val cameraId: String,
    val isFrontFacing: Boolean,
    val sensorSize: SizeF,
    val fovHorizontal: Float,
    val fovVertical: Float
)
