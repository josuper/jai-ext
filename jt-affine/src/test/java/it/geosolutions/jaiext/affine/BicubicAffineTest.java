package it.geosolutions.jaiext.affine;

import org.junit.Ignore;
//import org.junit.Ignore;
import org.junit.Test;

/**
 * This test-class extends the TestAffine class and is used for testing the bicubic interpolation inside the Affine operation.
 * The first method tests the affine operation without the presence of a ROI or a No Data Range. The 2nd method introduces a ROI 
 * object calculated using a ROI RasterAccessor while the 3rd method uses an Iterator on the ROI Object. The last method performs 
 * the affine operation with all the components. The last method is similar to the 4th method but executes its operations on binary 
 * images.
 */
public class BicubicAffineTest extends TestAffine{
    
    @Test
    public void testImageAffine() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
        boolean setDestinationNoData = true;      
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.MAGNIFY);
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testImageAffineROIAccessor() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;
              
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.MAGNIFY);
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testImageAffineROIBounds() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
        boolean setDestinationNoData = true;
              
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ONLY_DATA,ScaleType.MAGNIFY);
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.ROI_ONLY_DATA,ScaleType.REDUCTION);
    }
    
    @Test
    public void testImageAffineTotal() {        
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;              
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }
    
    @Test
    @Ignore
    public void testImageAffineBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;
                      
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }
 
}