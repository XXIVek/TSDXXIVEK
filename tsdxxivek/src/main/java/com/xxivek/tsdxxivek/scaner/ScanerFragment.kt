@file:Suppress("DEPRECATION")

package com.xxivek.tsdxxivek.scaner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.mlkit.codelab.translate.utilScaner.ImageUtils.convertYuv420888ImageToBitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.R
import com.xxivek.tsdxxivek.dataDB.AddItemFragment
import com.xxivek.tsdxxivek.dataDB.InventoryViewModel
import com.xxivek.tsdxxivek.dataDB.InventoryViewModelFactory
import com.xxivek.tsdxxivek.dataDB.Item
import com.xxivek.tsdxxivek.databinding.FragmentScanerBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ScanerFragment : Fragment() {
    private var _binding: FragmentScanerBinding? = null
    private val binding get() = _binding!!
    private lateinit var instBindle:Bundle

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    // to share the ViewModel across fragments.
    private val viewModelDB: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as TSDXXIVekApplication).database.itemDao()
        )
    }

    lateinit var resItem:Item
    private var resInput:Int=0
    private var resIncrement=1
    private var redyScan=1
    private var manualInputSh:Boolean = false

    //PreviewView  —  это кастомный View, который позволяет отображать ленту камеры.
    // Он был спроектирован, чтобы уменьшить нагрузку по настройке
    // и обработке поверхности превью, которая используется камерой.
    private lateinit var previewViewAct: PreviewView
    //В этом случае наблюдаемая операция является ProcessCameraProvider.
    //Этот процесс будет использоваться для привязки жизненного цикла камеры
    // к жизненному циклу приложения.
    private lateinit var camera: Camera
    private var laitFlash=false
    private var valZoom=0.4f
    private var cameraProvider: ProcessCameraProvider? = null
    //Добавьте cameraSelector атрибут, который поможет решить,
    // использовать ли переднюю или заднюю камеру
    //Устанавливаем заднюю камеру
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    //
    private var previewUseCase: Preview? = null
    //
    private var analysisUseCase: ImageAnalysis? = null

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentScanerBinding.inflate(inflater, container, false)
        redyScan=1
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manualInputSh=false
        previewViewAct  = binding.previewView
        with(previewViewAct) { setScaleType(PreviewView.ScaleType.FILL_CENTER) }

        binding.tvScannedType.text = "0"
        binding.tvScannedData.text = "Штрих код не распознан"
        binding.layoutScanResultRab.visibility=View.GONE
        binding.layoutScanErr.visibility=View.GONE
        binding.layoytInput.visibility=View.GONE
        binding.layoutScanResultTest.visibility=View.GONE

        binding.bFlash.setOnClickListener {onFlash()}
        binding.buttonScan.setOnClickListener {onScan()}

        binding.numberPickerSetincrement.minValue = 1
        binding.numberPickerSetincrement.maxValue = 20
        binding.numberPickerSetincrement.wrapSelectorWheel = false

        binding.numberPickerSetincrement.setOnValueChangedListener { picker, oldVal, newVal ->
            resIncrement=newVal
            resInput=resIncrement
            binding.textRabRes.text=resInput.toString()
        }

        binding.fabRabConfirm.setOnClickListener {
            onConfirm()
        }
        binding.fabRabNotConfirm.setOnClickListener {
            onScan()
        }
        binding.bErrScan.setOnClickListener {onScan()}
        binding.bErrSave.setOnClickListener {
            binding.root.findNavController()
                .navigate(R.id.action_scanerFragment_to_addItemFragment, instBindle)
        }
        binding.bSkipIncrement.setOnClickListener {
            resIncrement=1
            resInput=1
            binding.textRabRes.text=resIncrement.toString()
            binding.numberPickerSetincrement.value=1
        }
        binding.fabRabMin.setOnClickListener {changeResInput(-1)}
        binding.fabRabPlus.setOnClickListener {changeResInput(1)}
        binding.textRabRes.setOnClickListener {
            binding.layoutScanResultRab.visibility=View.GONE
            binding.layoytInput.visibility=View.VISIBLE
            binding.textInputRes.text="0"
         }

//работа клавиатуры
        binding.bInput1.setOnClickListener {setNum("1")}
        binding.bInput2.setOnClickListener {setNum("2")}
        binding.bInput3.setOnClickListener {setNum("3")}
        binding.bInput4.setOnClickListener {setNum("4")}
        binding.bInput5.setOnClickListener {setNum("5")}
        binding.bInput6.setOnClickListener {setNum("6")}
        binding.bInput7.setOnClickListener {setNum("7")}
        binding.bInput8.setOnClickListener {setNum("8")}
        binding.bInput9.setOnClickListener {setNum("9")}
        binding.bInput0.setOnClickListener {setNum("0")}
        binding.bInput00.setOnClickListener {setNum("00")}
        binding.bInputPoint.setOnClickListener {} //пока не знаю надо или нет
        binding.bInputClear.setOnClickListener {
            binding.textInputRes.text="0"
        }
        binding.bInputMinus.setOnClickListener {
            binding.textInputRes.text="-"+binding.textInputRes.text
        }
        binding.bInputEnter.setOnClickListener {fInputEnter()}
        binding.bManualinputSh.setOnClickListener {fManualinputSh()}

// Управление фокусом
        binding.previewView.setOnClickListener {
//            if (camera.cameraControl!=null) {
                if (valZoom == 0.7f) {
                    valZoom = 0.4f
                } else {
                    valZoom = 0.7f
                }
                camera.cameraControl.setLinearZoom(valZoom)
//            }
        }

         setupCamera()

    }
    private fun fManualinputSh(){
        manualInputSh=true
        binding.layoytBlank.visibility=View.GONE
        binding.layoytInput.visibility=View.VISIBLE
        binding.textInputRes.text="0"
        redyScan=0
    }
    private fun fInputEnter(){
        if (manualInputSh) {
            val manInpSh=binding.textInputRes.text.toString()
            binding.layoytInput.visibility = View.GONE
            manualInputSh=false
            barcodeAnalysis(manInpSh,1)
        }else{
            resInput = binding.textInputRes.text.toString().toInt()
            binding.layoytInput.visibility = View.GONE
            if (resInput == 0) {
                binding.textRabRes.text = resIncrement.toString()
            } else {
                binding.textRabRes.text = resInput.toString()
            }
            binding.textInputRes.text = "0"
            binding.layoutScanResultRab.visibility = View.VISIBLE
        }
    }
    private fun setupCamera() {
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(TSDXXIVekApplication())
        ).get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(viewLifecycleOwner) { provider: ProcessCameraProvider? -> cameraProvider = provider
               bindCameraUseCases()
            }
    }

    private fun setNum(text_num:String){
        var textInputRes=binding.textInputRes.text.toString()
        if ((text_num=="0")||(text_num=="00")) {
            if (textInputRes != "0") {
                textInputRes += text_num
            }
        }
        else{
            if (textInputRes == "0") {
                textInputRes = text_num
            } else {
                textInputRes += text_num
            }
        }
        binding.textInputRes.text=textInputRes
    }

    private fun onScan() {
        binding.tvScannedType.text = "0"
        binding.tvScannedData.text = "Штрих код не распознан"
        redyScan=1
        manualInputSh=false
        bindPreviewUseCase()
        bindAnalyseUseCase()
        if (appLic.appLIC=="-1"){
            binding.layoutScanResultTest.visibility=View.VISIBLE
            binding.layoytBlank.visibility=View.GONE
            binding.layoutScanResultRab.visibility=View.GONE
        }else {
            binding.textBlank.text="Наведите камеру на штрихкод"
            binding.bManualinputSh.visibility=View.VISIBLE
            binding.layoytBlank.visibility=View.VISIBLE
            binding.layoutScanResultRab.visibility=View.GONE
            binding.layoutScanResultTest.visibility=View.GONE
        }
    }

    private fun onFlash(){
        laitFlash=!laitFlash
        camera.cameraControl.enableTorch(laitFlash)
    }

    private fun onConfirm(){
        binding.fabRabConfirm.visibility=View.INVISIBLE
        binding.fabRabNotConfirm.visibility=View.INVISIBLE
        if ((resItem.itemSh.isNotBlank())&&(resInput!=0)) {
            var mRes=resItem.itemQuantity
            mRes+=resInput
            viewModelDB.addNewItem(
                resItem.itemSh,
                resItem.itemShTip.toString(),
                resItem.itemName,
                resItem.itemPrice.toString(),
                resItem.itemQuantityInStock.toString(),
                mRes.toString()
            )
        }
        onScan()
    }

    //строем два блока Preview и Analyse
    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        val cameraControl=camera.cameraControl
//        cameraControl.setZoomRatio(0.1f)
        cameraControl.setLinearZoom(valZoom)
//        val cameraInfo = camera.cameraInfo
//        cameraInfo.zoomState.observe(viewLifecycleOwner, Observer { zoomState ->
//            // Use the zoom state to retireve information about the zoom ratio, etc
//            val currentZoomRatio = zoomState.zoomRatio
//            // ...
//        })
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            previewViewAct.width.toFloat(), previewViewAct.height.toFloat())
        val centerWidth = previewViewAct.width.toFloat() / 2
        val centerHeight = previewViewAct.height.toFloat() / 2
        //create a point on the center of the view
        val autoFocusPoint2 = factory.createPoint(centerWidth, centerHeight)
        try {
            camera.cameraControl.startFocusAndMetering(
                FocusMeteringAction.Builder(
                    autoFocusPoint2,
                    FocusMeteringAction.FLAG_AF
                ).apply {
                    //auto-focus every 1 seconds
                    setAutoCancelDuration(1, TimeUnit.SECONDS)
                }.build()
            )
        } catch (e: CameraInfoUnavailableException) {
            Log.d("ERROR", "cannot access camera", e)
        }
        //create a point on the center of the view
        val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
//        val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
//            .createPoint(.5f, .5f)
//        val autoFocusAction = FocusMeteringAction.Builder(
//            autoFocusPoint,
//            FocusMeteringAction.FLAG_AF
//        ).apply {
//            //start auto-focusing after 2 seconds
//            setAutoCancelDuration(5, TimeUnit.SECONDS)
//        }.build()
//        cameraControl.startFocusAndMetering(autoFocusAction)
        bindAnalyseUseCase()
    }

    //Построение предпросмотра
    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        // окно предпросмотро создано, удаляем его
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }
        // строим новое окно предпросмотра
        previewUseCase = Preview.Builder()
            .setTargetRotation(previewViewAct.display.rotation)
//                .setTargetResolution(Size(600,450))//screenSize)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(Size(1920,1080))//screenSize)
            .build()
        previewUseCase!!.setSurfaceProvider(previewViewAct.surfaceProvider)

        try {
            camera=cameraProvider!!.bindToLifecycle(
                this,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    //Построение анализатора изображения
    private fun bindAnalyseUseCase() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setMaxResolution(Size(720,1024))
            .setTargetResolution(Size(720, 1024))
            .setTargetRotation(previewViewAct.display.rotation)
            .build()
        // Инициализируем нашего фонового исполнителя
        val cameraExecutor = Executors.newSingleThreadExecutor()

        //подставляем кадр анализатору для разбора
        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            { imageProxy ->
                analysisCodeProces(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }

        if (appLic.appLIC=="-1"){
            binding.layoutScanResultTest.visibility=View.VISIBLE
            binding.layoytBlank.visibility=View.GONE
            binding.layoutScanResultRab.visibility=View.GONE
        }else {
            binding.textBlank.text="Наведите камеру на штрихкод"
            binding.bManualinputSh.visibility=View.VISIBLE
            binding.layoutScanResultRab.visibility=View.GONE
            binding.layoutScanResultTest.visibility=View.GONE
            binding.layoytBlank.visibility=View.VISIBLE
        }

    }

    fun Bitmap.invertColors(): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val matrixInvert = ColorMatrix().apply {
            set(
                floatArrayOf(
                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                )
            )
        }

        val paint = Paint()
        ColorMatrixColorFilter(matrixInvert).apply {
            paint.colorFilter = this
        }

        Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
        return bitmap
    }

    // передача тест фото анализатору
    private fun analysisCodeProces(barcodeScanner:BarcodeScanner, imageProxy: ImageProxy){
        val image: Image = imageProxy.image!!
        @SuppressLint("UnsafeOptInUsageError")
        if (redyScan==1) {
            val bitMap1 = convertYuv420888ImageToBitmap(image)
            imageProxy.close()
            image.close()

            bitMap1.let {
                processImage(barcodeScanner, bitMap1)
                var bitMapN: Bitmap? = null
                bitMap1.apply {
                    invertColors()?.apply {
                        bitMapN = this
                    }
                }
                processImage(barcodeScanner, bitMapN!!)
            }
            //           imageProxy.close()
         }
        else {
            imageProxy.close()
            image.close()
        }
    }

    private fun processImage(barcodeScanner:BarcodeScanner, bitMap : Bitmap
    ){
        val inputImage = InputImage.fromBitmap(bitMap,0)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (redyScan==1) {
                    binding.tvScannedType.text = "0"
                    binding.tvScannedData.text = "Штрих код не распознан"
                    resInput=0
                    binding.layoutScanResultRab.visibility=View.GONE
                    binding.layoutScanErr.visibility=View.GONE
                    binding.layoutCamera.setBackgroundColor(super.
                        getResources().getColor(R.color.app_fon))

                    barcodes.forEach { barcode ->

                        val rawValue = barcode.rawValue
                        val valueType = barcode.valueType

                        binding.layoutCamera.setBackgroundColor(super.getResources().getColor(R.color.ok))
                        redyScan = 0
                        if (appLic.appLIC=="-1"){
                            val mText=rawValue.toString()
                            if (mText.startsWith("Lic")) {
                                appLic.saveLic(mText)
                                val navControler = binding.root.findNavController()
                                //cameraProvider?.shutdown()
                                navControler.navigate(R.id.action_scanerFragment_to_pairingFragment)
                            }else{
                                binding.tvScannedType.text = valueType.toString()
                                binding.tvScannedData.text =rawValue
                            }
                        }else {
                            if (!manualInputSh) {
                                barcodeAnalysis(rawValue.toString(), valueType)
                            }
                        }

//                        // Полный список поддерживаемых типов см. в ссылке на API
//                        when (valueType) {
//                            Barcode.TYPE_WIFI -> {
//                                val ssid = barcode.wifi!!.ssid
//                                val password = barcode.wifi!!.password
//                                val type = barcode.wifi!!.encryptionType
//                                binding.tvScannedData.text =
//                                    "ssid: $ssid\npassword: $password\ntype: $type"
//                            }
//                            Barcode.TYPE_URL -> {
//                                val title = barcode.url!!.title
//                                val url = barcode.url!!.url
//
//                                binding.tvScannedData.text = "Title: " + title + "\nURL: " + url
//                            }
//                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, it.message ?: it.toString())
            }
            .addOnCompleteListener {
                // Если изображение получено из варианта использования анализа камеры,
                // необходимо вызвать image.close() для полученных изображений
                // после завершения их использования.
                // В противном случае новые изображения могут не быть получены
                // или камера может по-прежнему работать.
//                imageProxy.close()
            }
    }

    private fun barcodeAnalysis(shtrih:String,shtrihTip:Int){
        val result = BarcodeConversions().conversions(shtrih,shtrihTip)
        barcodeGetItem(result)
    }
    private fun barcodeGetItem(result:Map<String,String>){
        CoroutineScope(IO).launch() {
            var tekItem:Item?=null
            var mShtrih=result["shtrih"]!!
            var mShtrihTip=result["shtrihTip"]!!
            var accept=result["accept"]!!
            if (accept=="1") {
                tekItem = itemDatabase?.getItem2(mShtrih)
            }
            withContext(Dispatchers.Main) {displayResult(tekItem,mShtrih,mShtrihTip,accept)}
        }
    }
    fun displayResult(data:Item?,m_Shtrih:String,mShtrihTip:String,accept:String){
        if (data==null) {
            if (accept=="1") {
                if ((m_Shtrih.length==150)||(m_Shtrih.length==68)){
                    binding.textErr.text = "Алкогольная марка\n не найдена!"
                }else{
                    if (appLic.appOper=="1"){
                       binding.bErrSave.visibility=View.VISIBLE
                        instBindle=newInstance(m_Shtrih,mShtrihTip)
                    }else{
                        binding.bErrSave.visibility=View.GONE
                    }
                    binding.textErr.text = "Штрихкод \n" + m_Shtrih + "\n" + "не найден!"
                }
            }else{
                binding.textErr.text = "Штрихкод считан неверно.\n Повторите операцию."
            }
            binding.layoutScanErr.visibility = View.VISIBLE
            binding.layoytBlank.visibility = View.GONE
        }else{
            if (data.itemName.isNotBlank()) {
                resItem=data
                resInput=resIncrement
                binding.textRabSh.text = resItem.itemSh
                binding.textRabNaim.text = resItem.itemName
                if ((appLic.appOper=="2")||(appLic.appOper=="3")){
                    binding.textRabCount.text = ""+resItem.itemQuantity.toString() +"/"+resItem.itemQuantityInStock.toString()
                }else{
                    binding.textRabCount.text = resItem.itemQuantity.toString()
                }
                binding.textRabRes.text=resInput.toString()
                binding.fabRabConfirm.visibility=View.VISIBLE
                binding.fabRabNotConfirm.visibility=View.VISIBLE
                binding.layoutScanResultRab.visibility = View.VISIBLE
                binding.layoytBlank.visibility = View.GONE
            }
        }
    }
    private fun changeResInput(changeRes:Int){
        resInput+=changeRes
        binding.textRabRes.text=resInput.toString()
    }

    override fun onStop() {
        redyScan=0
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setupCamera()
    }

    companion object {

        private val TAG = ScanerFragment::class.java.simpleName
        @JvmStatic
        fun newInstance(mARG_SH: String,mARG_SHTIP:String):Bundle {
            val bundle = Bundle()
            bundle.putString("ARG_SH",mARG_SH)
            bundle.putString("ARG_SHTIP",mARG_SHTIP)
            bundle.putString("ARG_NAME","")
            bundle.putString("ARG_PRICE","0.0")
            bundle.putString("ARG_QUANTITYINSTOCK","0")
            bundle.putString("ARG_QUANTITY","1")
            bundle.putString("ARG_VIEW","ScanerFragment")
            return bundle
        }

    }
}