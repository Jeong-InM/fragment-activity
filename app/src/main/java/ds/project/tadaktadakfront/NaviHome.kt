package ds.project.tadaktadakfront

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.FileProvider
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.fragment_navi_home.*
import kotlinx.android.synthetic.main.fragment_navi_home.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Collections.rotate


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class NaviHome : Fragment() {

    private var param1: String? = null
    private var param2: String? = null


    var uri: Uri? = null //원본이미지 Uri를 저장할 변수
    lateinit var mContext: Context
    lateinit var currentPhotoPath: String //imagePath


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)

        }
        AndPermission.with(this)//권한 라이브러리
            .runtime()
            .permission(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.CAMERA
            )
            .onGranted { permissions ->
                Log.d("Main", "허용된 권한 갯수 : ${permissions.size}")
            }
            .onDenied { permissions ->
                Log.d("Main", "거부된 권한 갯수 : ${permissions.size}")
            }
            .start()

    }

    override fun onCreateView( //뷰를 생성하는 메소드
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_navi_home, container, false)

        view.btnCamera.setOnClickListener {
            takePhoto()
        }
        view.btnGallery.setOnClickListener {
            getFromAlbum()
        }
        view.btnselect.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val pass = getView()?.findViewById<ImageButton>(R.id.btnselect)
                pass?.setOnClickListener {
                    if (select_ImageView != null) {
                        val intent = Intent(activity, SetImageActivity::class.java)
                        intent.putExtra("path", currentPhotoPath)
                        activity!!.startActivity(intent)
                    }
                }
            }
        })


        return view
    }


    fun takePhoto() { //카메라 인텐트 생성
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                //결과가 null일 때, startActivity를 호출하면 앱이 비정상 종료됨.
                //그래서 resolveActivity는 결과가 null이 아닌지 확인. 즉, 앱 사망 방지

                val photoFile: File? = try { //사진을 저장할 파일 만들기
                    createImageFile()
                } catch (ex: IOException) { //파일을 만드는 동안 오류가 발생한 경우
                    null
                }
                photoFile?.also {//성공적으로 파일이 생성된 경우에만 계속 처리
                    val photoURI: Uri = FileProvider.getUriForFile(
                        mContext,
                        "ds.project.tadaktadakfront.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, 101)
                }
            }
        }
    }


    fun getFromAlbum() { //갤러리 인텐트 생성
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 102)
    }


    @Throws(IOException::class)
    private fun createImageFile(): File { //파일명 중복방지 임시 파일 생성

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        //날짜-시간 스탬프를 사용하여 새 사진의 고유한 파일 이름을 반환하는 메서드
        val storageDir: File? =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // 카메라로 캡쳐한 사진은 기기의 공용 외부 저장소에 저장, 모든 앱에서 액세스할 수 있고
        // DIRECTORY_PICTURES를 인수로 사용하여 getExternalStoragePublicDirectory()에서 제공

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            //경로저장: ACTION_VIEW intents에 사용할 파일
            currentPhotoPath = absolutePath
        }
    }

    //startActivityForResult를 통해서 기본 카메라 앱으로 부터 받아온 결과값으로 이미지뷰에 전달
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK) {//카메라 이미지 찍고, 권한도 승인되면 이미지 처리
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                lateinit var exif: ExifInterface
                //ExifInterface(): 이미지 데이터를 제외한 이미지를 설명해주는 메타데이터 포멧

                try { //try-catch 문을 이용하여 촬영한 이미지의 정보를 받아옴.
                    exif = ExifInterface(currentPhotoPath)
                    var exifOrientation = 0
                    var exifDegree = 0

                    if (exif != null) {
                        exifOrientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        exifDegree = exifOrientationToDegress(exifOrientation)
                        //exifOrientationToDegress()함수를 이용해 회전된 값을 추출
                    }

                    select_ImageView.setImageBitmap(rotate(bitmap, exifDegree))
                    //rotate() 함수를 이용해서 원래의 방향으로 회전
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else if (requestCode == 102) {//갤러리 이미지 선택, 권한도 승인되면 이미지 처리
            if (resultCode == Activity.RESULT_OK) {
                uri=data?.data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {//bitmap sdk 최소 버전 이상시 처리
                    if (uri != null) {
                        try {
                            val bitmap =
                                BitmapFactory.decodeStream(
                                    requireActivity().contentResolver.openInputStream(
                                        uri!!
                                    )
                                )
                            select_ImageView.setImageBitmap(bitmap)
                            currentPhotoPath= Uri.parse(data!!.dataString).toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            }
        }
    }


    override fun onAttach(activity: Activity) { //메인 context 자유롭게 사용
        super.onAttach(activity)
        if (context is MainActivity) {
            mContext = context as MainActivity
        }
    }


    private fun exifOrientationToDegress(exifOrientation: Int): Int {
        //ExifInterface()를 이용해 사진이 90도, 180도, 270도 돌아갔는지 파악.
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                Log.d("rotate", "rotate90")
                return 90
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                Log.d("rotate", "rotate180")
                return 180
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                Log.d("rotate", "rotate270")
                return 270
            }
            else -> {
                Log.d("rotate", "rotate0")
                return 0
            }

        }
    }

    private fun rotate(bitmap: Bitmap, degree: Int): Bitmap { // Matrix 기능을 이용해 실제 사진을 원래대로 회전
        //내가 촬영했던 사진을 Bitmap 형식으로 가지고 있는 bitmap 변수를
        // exifOrientationToDegress() 함수에서 리턴된 값을 degree로 가져와서 처리.
        Log.d("rotate", "init rotate")
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    companion object {

        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NaviHome().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}