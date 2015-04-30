package monitor.pack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.util.Log;

public class Line {
	//Shaders
	private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            // the matrix must be included as a modifier of gl_Position
            // Note that the uMVPMatrix factor *must be first* in order
            // for the matrix multiplication product to be correct.
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

	private final String fragmentShaderCode =
	    "precision mediump float;" +
	    "uniform vec4 vColor;" +
	    "void main() {" +
	    "  gl_FragColor = vColor;" +
	    "}";
	private FloatBuffer mVertexBuffer;
 	private final int mProgram;
 	private int mPositionHandle;
 	private int mColorHandle;
 	private int mMVPMatrixHandle;
 	private int mPos = 0;
 	private boolean mGap;
 	private boolean mFill = false;
 	private boolean mDraw = true;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    private int mResolution;
    private float mLineCoords[];
    private int mVertexCount;
    private final int mVertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    private float mColor[] = { 1F, 0.0F, 0.0f, 1.0f };
    
    
    //Constructor
    public Line(int res, boolean gap) {
    	mGap = gap;
    	mResolution = res;
    	mLineCoords = new float[mResolution*3];
    	mVertexCount = mLineCoords.length / COORDS_PER_VERTEX;
    	for(int i = 0; i < mResolution;i++)
    	{
    		mLineCoords[i*3] = -1F + (2f * i / (mResolution -1));
    		mLineCoords[i*3+1] = 0F;
    		mLineCoords[i*3+2] = 0F;
    	}
        // initialize vertex byte buffer for shape coordinates
    	ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
        		(mLineCoords.length + 3) * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        mVertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        mVertexBuffer.put(mLineCoords);
        // set the buffer to read the first coordinate
        mVertexBuffer.position(0);
     // prepare shaders and OpenGL program
        int vertexShader = GLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }
    
    //Set Color of Line
    public void setColor(float r, float g, float b)
    {
    	mColor[0] = r;
    	mColor[1] = g;
    	mColor[2] = b;
    }
    
    //Set new Point received from the SignalServer
    public void setValue(float val)
    {
    	mVertexBuffer.clear();
    	
    	mLineCoords[mPos * 3 + 1] = val;
    	mVertexBuffer.put(mLineCoords);
        // set the buffer to read the first coordinate
        mVertexBuffer.position(0);
    	mPos++;
    	if(mPos >= mResolution)
        	mPos = 0;
    }
    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
    	if(!mDraw)
    		return;
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                mVertexStride, mVertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        //Check if we need a small gap
        if(mGap)
        {
        	if(mFill)
        	{
        		int i = 0;
        		while(i < mResolution - 1)
        		{
        			
        			mVertexBuffer.clear();
        			int start = i;
        			//Nothing to Fill
        			if(mLineCoords[i*3+1] == 0)
        			{
        				//Loop through all points which equals 0
        				while( i < mResolution - 1 && mLineCoords[i*3+1] == 0 && !(i >= mPos && i < mPos + mResolution/100))
        					i++;
        				
        				mVertexBuffer.put(mLineCoords,start*3,(i-start)*3);
        		        mVertexBuffer.position(0);
        		    	if(mPos >= mResolution)
        		        	mPos = 0;
        				GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, (i - start));
        			}else{
        				//We got something to fill
        				//Add Vertex as central Point of all Triangles
        				mVertexBuffer.put(new float[]{-1F + (2f * i / (mResolution -1)),0,0});
	        			while(i < mResolution -1 && mLineCoords[i*3+1] != 0 && !(i >= mPos && i < mPos + mResolution/100))
	        				i++;
	        			mVertexBuffer.put(mLineCoords,start*3,(i-start)*3);
	        			//Add HelperVertex to fill whole Curve
	        			mVertexBuffer.put(new float[]{-1F + (2f * i / (mResolution -1)),0,0});
        		        mVertexBuffer.position(0);
        		        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, (i - start)+2);
        		        //We need at least 3 Vertexes for a triangle
        		        if(i - start > 2)
	        				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, (i - start)+2);
        			}
        			while(i >= mPos && i < mPos + mResolution/100)
        				i++;
        		}
        	}else{
		        int startPos = mPos + mResolution/100;
		        int tempPos = startPos;
		        //Check if the gap is at the beginning of the line
		        startPos = startPos >= mResolution ? startPos - mResolution : 0;
		        //Draw Line before Gap
		        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, startPos, mPos - startPos);
		        //Check if we need a line after gap
		        if(tempPos < mResolution)
		        	//Draw Line after Gap
		        	GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, tempPos, mVertexCount - tempPos);
        	}
        }else //No GAP
        	GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mVertexCount);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        
    }
    
    //Draw/Hide Line
    public void setDrawAble(boolean draw)
    {
    	mDraw = draw;
    }
    
    //Draw getter
    public boolean getDrawAble()
    {
    	return mDraw;
    }
    
    //Enable/Disable Linefilling
    public void setFill(boolean fill)
    {
    	mFill = fill;
    }
}
