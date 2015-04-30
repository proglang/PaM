package monitor.pack;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

public class GLRenderer implements Renderer {
	public enum LineType{Background, Heart,Blood,O2,CO2,AF,Trenner}
	private Line mLineHeart;
	private Line mLineBlood;
	private Line mLineO2;
	private Line mLineCO2;
	private Line mLineAF;
	private Line mLineTrenner;
	private int mWidth;
	private int mHeight;
    private float mbgColor[] = { 0.0F, 0.0F, 0.0f, 1.0f };
	private double mLastUpdate = -1;
	private double mDeltaTime = 0;
	private Signalserver mSignalServer;
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    
    public GLRenderer()
    {
    	
    }
    
    //Loads OpenGL Shaders
	static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);

	    return shader;
	}
	
	//Computes elapsed Time since last frame
	private double getDeltaTime()
	{
		if(mLastUpdate == -1)
		{
			mLastUpdate = System.nanoTime();
			return 0;
		}
		double currentTime = System.nanoTime();
		double delta = currentTime - mLastUpdate;
		mLastUpdate = currentTime;
		return delta;
	}
	
	
	//Draw all Curves
	@Override
	public void onDrawFrame(GL10 gl) {
        mDeltaTime += getDeltaTime() / 1000000;
        // Draw background color
        GLES20.glClearColor(mbgColor[0], mbgColor[1], mbgColor[2], 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        gl.glLineWidth(3f);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 1, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        float[] drawMatrix = new float[16];
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        if (mSignalServer == null) return;
        //Get Data from SignalServer
        while(mDeltaTime > 1000f / 60f )
        {
        	mDeltaTime -= 1000f / 60f;
        	//EKG
        	if(mLineHeart.getDrawAble())
        		mLineHeart.setValue((float)mSignalServer.getHeartRateValue() / 250);
        	else
        		mLineHeart.setValue(0);
        	//Bloodpressure
        	if(mLineBlood.getDrawAble())
        		mLineBlood.setValue((float)mSignalServer.getBloodPressureValue() / 170);
        	else
        		mLineBlood.setValue(0);
        	//ETCO2
        	if(mLineCO2.getDrawAble())
        		//mLineCO2.setValue(50);
        		mLineCO2.setValue((float)mSignalServer.getCO2Value() / 250);
        	else
        		mLineCO2.setValue(0);
        	//SpO2
        	if(mLineO2.getDrawAble())
        		mLineO2.setValue((float)mSignalServer.getO2Value() / 250);
        	else
        		mLineO2.setValue(0);
	        mSignalServer.increment();
        }
        
        //Scissor Test for Curves
        gl.glEnable(GL10.GL_SCISSOR_TEST);
        // Draw EKG Line
        gl.glScissor(0, (int)(mHeight * 0.71F), mWidth, (int)(mHeight * (0.29F)));
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, 0.55F, 0F);
        mLineHeart.draw(drawMatrix);
        
        
        //Draw Bloodpressure Line
        gl.glScissor(0, (int)(mHeight * 0.48F), mWidth, (int)(mHeight * (0.23F)));
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, -0.11F, 0F);
        mLineBlood.draw(drawMatrix); //Translation = 0
        
        //Draw  O2 Line
        gl.glScissor(0, (int)(mHeight * 0.24F), mWidth, (int)(mHeight * (0.23F)));
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, -0.52F, 0F);
        mLineO2.draw(drawMatrix);
        
        gl.glScissor(0, 0, mWidth, (int)(mHeight * (0.23F)));
        //Draw CO2 Line
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, -0.99F, 0F);
        mLineCO2.draw(drawMatrix);
        
        gl.glDisable(GL10.GL_SCISSOR_TEST);
        //Trennlinien zwischen den einzelnen Kurven
        gl.glLineWidth(2f);
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, 0.415F, 0F);
        mLineTrenner.draw(drawMatrix);
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, -0.056F, 0F);
        mLineTrenner.draw(drawMatrix);
        drawMatrix = mMVPMatrix.clone();
        Matrix.translateM(drawMatrix, 0, 0F, -0.528F, 0F);
        mLineTrenner.draw(drawMatrix);
	}

	//Called if Size of RenderWindow changes
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        
        Matrix.orthoM(mProjectionMatrix, 0, -1, +1, -1, 1, 1, 7);
        
        
	}
	
	//Change Color of given Curve in r,g,b(0-1)
	public void SetColor(LineType line, float r,float g,float b)
	{
		if(line == LineType.AF)
			mLineAF.setColor(r, g, b);
		if(line == LineType.Blood)
			mLineBlood.setColor(r, g, b);
		if(line == LineType.CO2)
			mLineCO2.setColor(r, g, b);
		if(line == LineType.Heart)
			mLineHeart.setColor(r, g, b);
		if(line == LineType.O2)
			mLineO2.setColor(r, g, b);
		if(line == LineType.Trenner)
			mLineTrenner.setColor(r,g,b);
		if(line == LineType.Background)
		{
			mbgColor[0] = r;
			mbgColor[1] = g;
			mbgColor[2] = b;
		}
			
	}
	
	//Enable/Disable Line
	public void ToogleLine(LineType line, boolean draw)
	{
		if(line == LineType.AF)
			mLineAF.setDrawAble(draw);
		if(line == LineType.Blood)
			mLineBlood.setDrawAble(draw);
		if(line == LineType.CO2)
			mLineCO2.setDrawAble(draw);
		if(line == LineType.Heart)
			mLineHeart.setDrawAble(draw);
		if(line == LineType.O2)
			mLineO2.setDrawAble(draw);
	}

	//Initialze RenderWindow and Curves
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Clear Framebuffer
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //Enable Linesmoothing
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        //Init HeartLine
        if(mLineHeart == null)
        {
        	mLineHeart = new Line(500,true);
        	mLineHeart.setColor(0f, 1f, 0f);
        }
        //Init BloodLine
        if(mLineBlood == null)
        {
        	mLineBlood = new Line(500,true);
        	mLineBlood.setColor(1f, 0f, 0f);
        }
        //Init O2Line
        if(mLineO2 == null) {
        	mLineO2 = new Line(500,true);
        	mLineO2.setColor(1f, 1f, 0f);
        }
        //Init CO2Line
        if(mLineCO2 == null)
        {
        	mLineCO2 = new Line(500,true);
        	mLineCO2.setColor(0.5f, 0.5f, 0.5f);
        	mLineCO2.setFill(true);
        }
        //Init AFLine
        if(mLineAF == null) {
        	mLineAF = new Line(500,true);
        	mLineAF.setColor(1f, 1f, 0f);
        }
        //Init Line between curves
        if(mLineTrenner == null)
        {
        	mLineTrenner = new Line(2,false);
        	mLineTrenner.setColor(0.1f, 0.1f, 0.1f);
        	mLineTrenner.setValue(0);
        	mLineTrenner.setValue(0);
        	mLineTrenner.setDrawAble(true);
        }
        
		
	}
	
	//Setter SignalServer
	public void setSignalserver(Signalserver s) {
		mSignalServer = s;
	}

}
