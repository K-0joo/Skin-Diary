package com.cso.skin_diary

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cso.skin_diary.databinding.ActivityDailyCareBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class DailyCare : AppCompatActivity() {

    //바인딩 객체 생성
    lateinit var binding: ActivityDailyCareBinding
    lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyCareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //카메라 권한 확인
        checkPermission()

        //camera request launcher.................
        //사진 데이터 가져오기
        val requestCameraFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){
            //카메라 앱
            val calRatio = calculateInSampleSize(
                Uri.fromFile(File(filePath)),
                resources.getDimensionPixelSize(R.dimen.imgSize),
                resources.getDimensionPixelSize(R.dimen.imgSize)
            )
            val option = BitmapFactory.Options()
            option.inSampleSize = calRatio



            var bitmap = BitmapFactory.decodeFile(filePath, option)

            //meta data 저장하는 Exif
            val exif = filePath?.let { ExifInterface(filePath) }
            val exifOrientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val exifDegree = exifOrientationToDegrees(exifOrientation)
            bitmap = rotate(bitmap, exifDegree)

            bitmap?.let {
                //Glide.with(this).load(bitmap).into(binding.frontCamera)
                binding.frontCamera.setImageBitmap(bitmap)
            }

        }


        binding.frontCamera.setOnClickListener {
            //카메라 권한 허용 확인

            //camera app......................
            //파일 준비...............
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            filePath = file.absolutePath
            //카메라 앱을 실행하는 인텐트
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.cso.skin_diary.fileprovider",
                file
            )
            //사진 촬영 액티비티 실행
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            requestCameraFileLauncher.launch(intent)
        }

    }//onCreate의 끝


    //권한 확인 소스
    fun checkPermission(){

        //1. 카메라 권한 승인 상태 가져오기
        val cameraPermission = ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED){
            //카메라 권한이 승인된 상태일때
            startProcess()
        } else {
            //카메라 권한이 승인되지 않았을 경우
            requestPermission()
        }
    }


    //2. 권한 요청
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),99)
    }


    //권한 요청청
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            99->{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startProcess()
                }else{
                    Log.d("DailyCare", "종료")
                }
            }
        }
    }


    fun startProcess(){
        Toast.makeText(this, "카메라 기능 권한 허용됨.", Toast.LENGTH_SHORT).show()
    }

    //사진 각도 반환 함수
    fun exifOrientationToDegrees(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    // 이미지 회전 함수
    private fun rotate(bitmap: Bitmap, degree: Int) : Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix,true)
    }

    //사진 이미지 재설정
    private fun calculateInSampleSize(fileUri: Uri, reqWidth: Int, reqHeight: Int): Int {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            var inputStream = contentResolver.openInputStream(fileUri)

            //inJustDecodeBounds 값을 true 로 설정한 상태에서 decodeXXX() 를 호출.
            //로딩 하고자 하는 이미지의 각종 정보가 options 에 설정 된다.
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream!!.close()
            inputStream = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //비율 계산........................
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        //inSampleSize 비율 계산
        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

}//class의 끝