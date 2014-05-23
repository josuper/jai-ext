package it.geosolutions.jaiext.affine;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.scale.ScaleBicubicOpImage;
import it.geosolutions.jaiext.scale.ScaleBilinearOpImage;
import it.geosolutions.jaiext.scale.ScaleGeneralOpImage;
import it.geosolutions.jaiext.scale.ScaleNearestOpImage;
import it.geosolutions.jaiext.translate.TranslateIntOpImage;
import it.geosolutions.jaiext.utilities.ImageUtilities;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import com.sun.media.jai.mlib.MlibAffineRIF;
import com.sun.media.jai.opimage.CopyOpImage;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * @since EA4
 * @see AffineOpimage, ScaleOpImage
 */
public class AffineCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public AffineCRIF() {
        super("affine");
    }

    /**
     * Creates an affine operation as an instance of AffineOpImage.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Get TileCache from renderHints if any.
        // TileCache cache = RIFUtil.getTileCacheHint(renderHints);

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);
        // Get the source image
        RenderedImage source = paramBlock.getRenderedSource(0);
        // Get the affine transformation
        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform transform = (AffineTransform) arg0;
        // Get the interpolation object
        Object arg1 = paramBlock.getObjectParameter(1);
        Interpolation interp = (Interpolation) arg1;
        // Get the background values
        double[] backgroundValues = (double[]) paramBlock.getObjectParameter(2);

        // Get the affine transform matrix
        double tr[];
        tr = new double[6];
        transform.getMatrix(tr);

        // Get the boolean useROIAccessor (by default set to false)
        boolean useROIAccessor = false;

        // SG make sure we use the ROI
        Object property = paramBlock.getObjectParameter(3);
        ROI roi = null;
        if (property instanceof ROI) {
            roi = (ROI) property;
            PlanarImage temp = PlanarImage.wrapRenderedImage(source);
            temp.setProperty("ROI", roi);
            source = temp;
            // If ROI is present then the ROI Accessor can be set to true.
            useROIAccessor = (Boolean) paramBlock.getObjectParameter(4);
        }

        try {
            // check if we can use the native operation instead
            Rectangle sourceBounds = new Rectangle(source.getMinX(), source.getMinY(),
                    source.getWidth(), source.getHeight());
            if (roi == null || (ImageUtilities.isMediaLibAvailable() && roi.contains(sourceBounds))) {
                RenderedImage accelerated = new MlibAffineRIF().create(paramBlock, renderHints);
                if (accelerated != null) {
                    return accelerated;
                }
            }
        } catch (Exception e) {
            // Eat exception and proceed with pure java approach
        }

        // Get the boolean setDestinationNoData
        boolean setDestinationNoData = (Boolean) paramBlock.getObjectParameter(5);
        // Get the image bounds
        Rectangle sourceBounds = new Rectangle(source.getMinX(), source.getMinY(),
                source.getWidth(), source.getHeight());

        // For remember
        // tr[0] = scale on the X axis
        // tr[1] = shear on the Y axis
        // tr[2] = shear on the X axis
        // tr[3] = scale on the Y axis
        // tr[4] = translate on the X axis
        // tr[5] = translate on the Y axis

        //
        // Check and see if the affine transform is doing a copy.
        // If so call the copy operation.This operation can
        // be executed only if ROI data are not defined or contain all the
        // image or are empty.
        //
        if ((tr[0] == 1.0) && (tr[3] == 1.0) && (tr[2] == 0.0) && (tr[1] == 0.0) && (tr[4] == 0.0)
                && (tr[5] == 0.0)
                && (roi == null || (roi.getBounds().isEmpty() || roi.contains(sourceBounds)))) {
            // It's a copy
            return new CopyOpImage(source, renderHints, layout);
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Translate operation. That is a scale by 1 and no rotation.
        // In which case call translate. Note that only integer translate
        // is applicable. For non-integer translate we'll have to do the
        // affine. This operation can be executed only if ROI data are not
        // defined or contain all the image or are empty.
        // If the hints contain an ImageLayout hint, we can't use
        // TranslateIntOpImage since it isn't capable of dealing with that.
        if ((tr[0] == 1.0) && (tr[3] == 1.0) && (tr[2] == 0.0) && (tr[1] == 0.0)
                && (Math.abs(tr[4] - (int) tr[4]) < TOLERANCE)
                && (Math.abs(tr[5] - (int) tr[5]) < TOLERANCE) && layout == null
                && (roi == null || (roi.getBounds().isEmpty() || roi.contains(sourceBounds)))) {
            // It's a integer translate
            return new TranslateIntOpImage(source, renderHints, (int) tr[4], (int) tr[5]);
        }

        // control if the image is binary
        SampleModel sm = source.getSampleModel();

        boolean isBinary =  (sm instanceof MultiPixelPackedSampleModel)
                && (sm.getSampleSize(0) == 1)
                && (sm.getDataType() == DataBuffer.TYPE_BYTE
                        || sm.getDataType() == DataBuffer.TYPE_USHORT || sm.getDataType() == DataBuffer.TYPE_INT);
        
        //
        // Check and see if the affine transform is in fact doing
        // a Scale operation. In which case call Scale which is more
        // optimized than Affine.
        //
        if ((tr[0] > 0.0) && (tr[2] == 0.0) && (tr[1] == 0.0) && (tr[3] > 0.0)) {
            // It's a scale
        	if (interp instanceof InterpolationNearest && !isBinary) {

                InterpolationNearest interpN = (InterpolationNearest) interp;

                return new ScaleNearestOpImage(source, layout, renderHints, extender, interpN,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);

            } else if (interp instanceof InterpolationNearest  && isBinary) {

                InterpolationNearest interpN = (InterpolationNearest) interp;

                return new ScaleGeneralOpImage(source, layout, renderHints, extender, interpN,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);

            }else if (interp instanceof InterpolationBilinear && !isBinary) {

                InterpolationBilinear interpB = (InterpolationBilinear) interp;

                return new ScaleBilinearOpImage(source, layout, renderHints, extender, interpB,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);
            } else if (interp instanceof InterpolationBilinear && isBinary) {

                InterpolationBilinear interpB = (InterpolationBilinear) interp;

                return new ScaleGeneralOpImage(source, layout, renderHints, extender, interpB,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);
            }else if (interp instanceof InterpolationBicubic && !isBinary) {
                InterpolationBicubic interpBN = (InterpolationBicubic) interp;

                return new ScaleBicubicOpImage(source, layout, renderHints, extender, interpBN,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);
            } else if (interp instanceof InterpolationBicubic && isBinary) {
                InterpolationBicubic interpBN = (InterpolationBicubic) interp;

                return new ScaleGeneralOpImage(source, layout, renderHints, extender, interpBN,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);
            } else {
                return new ScaleGeneralOpImage(source, layout, renderHints, extender, interp,
                        (float) tr[0], (float) tr[3], (float) tr[4], (float) tr[5], useROIAccessor);
            }
        }
        // Have to do Affine
        if (interp instanceof InterpolationNearest && !isBinary) {

            InterpolationNearest interpN = (InterpolationNearest) interp;

            return new AffineNearestOpImage(source, extender, renderHints, layout, transform, interpN, setDestinationNoData, useROIAccessor);

        } else if (interp instanceof InterpolationNearest && isBinary) {

            InterpolationNearest interpN = (InterpolationNearest) interp;

            return new AffineGeneralOpImage(source, extender, renderHints, layout, transform,
                    interpN, useROIAccessor, setDestinationNoData);

        } else if (interp instanceof InterpolationBilinear && !isBinary) {

            InterpolationBilinear interpB = (InterpolationBilinear) interp;

            return new AffineBilinearOpImage(source, extender, renderHints, layout, transform, interpB, backgroundValues, setDestinationNoData, useROIAccessor);

        }else if (interp instanceof InterpolationBilinear && isBinary) {

            InterpolationBilinear interpB = (InterpolationBilinear) interp;

            return new AffineGeneralOpImage(source, extender, renderHints, layout, transform,
                    interpB, useROIAccessor, setDestinationNoData);

        } else if (interp instanceof InterpolationBicubic && !isBinary ) {

            InterpolationBicubic interpBN = (InterpolationBicubic) interp;

            return new AffineBicubicOpImage(source, extender, renderHints, layout, transform, interpBN, backgroundValues, setDestinationNoData, useROIAccessor);
        }else if (interp instanceof InterpolationBicubic && isBinary) {

            InterpolationBicubic interpBN = (InterpolationBicubic) interp;

            return new AffineGeneralOpImage(source, extender, renderHints, layout, transform,
                    interpBN, useROIAccessor, setDestinationNoData);
        } else {
            return new AffineGeneralOpImage(source, extender, renderHints, layout, transform,
                    interp, backgroundValues, setDestinationNoData);
        }

    }

    /**
     * Creates a new instance of <code>AffineOpImage</code> in the renderable layer. This method satisfies the implementation of CRIF.
     */
    public RenderedImage create(RenderContext renderContext, ParameterBlock paramBlock) {
        return paramBlock.getRenderedSource(0);
    }

    /**
     * Maps the output RenderContext into the RenderContext for the ith source. This method satisfies the implementation of CRIF.
     * 
     * @param i The index of the source image.
     * @param renderContext The renderContext being applied to the operation.
     * @param paramBlock The ParameterBlock containing the sources and the translation factors.
     * @param image The RenderableImageOp from which this method was called.
     */
    public RenderContext mapRenderContext(int i, RenderContext renderContext,
            ParameterBlock paramBlock, RenderableImage image) {
        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform affine = (AffineTransform) arg0;

        RenderContext RC = (RenderContext) renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(affine);
        RC.setTransform(usr2dev);
        return RC;
    }

    /**
     * Gets the bounding box for the output of <code>AffineOpImage</code>. This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        RenderableImage source = paramBlock.getRenderableSource(0);
        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform forward_tr = (AffineTransform) arg0;

        // Get the affine transform
        double tr[];
        tr = new double[6];
        forward_tr.getMatrix(tr);

        //
        // Check and see if the affine transform is doing a copy.
        //
        if ((tr[0] == 1.0) && (tr[3] == 1.0) && (tr[2] == 0.0) && (tr[1] == 0.0) && (tr[4] == 0.0)
                && (tr[5] == 0.0)) {
            return new Rectangle2D.Float(source.getMinX(), source.getMinY(), source.getWidth(),
                    source.getHeight());
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Translate operation.
        //
        if ((tr[0] == 1.0) && (tr[3] == 1.0) && (tr[2] == 0.0) && (tr[1] == 0.0)
                && (Math.abs(tr[4] - (int) tr[4]) < TOLERANCE)
                && (Math.abs(tr[5] - (int) tr[5]) < TOLERANCE)) {
            return new Rectangle2D.Float(source.getMinX() + (float) tr[4], source.getMinY()
                    + (float) tr[5], source.getWidth(), source.getHeight());
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Scale operation.
        //
        if ((tr[0] > 0.0) && (tr[2] == 0.0) && (tr[1] == 0.0) && (tr[3] > 0.0)) {
            // Get the source dimensions
            float x0 = (float) source.getMinX();
            float y0 = (float) source.getMinY();
            float w = (float) source.getWidth();
            float h = (float) source.getHeight();

            // Forward map the source using x0, y0, w and h
            float d_x0 = x0 * (float) tr[0] + (float) tr[4];
            float d_y0 = y0 * (float) tr[3] + (float) tr[5];
            float d_w = w * (float) tr[0];
            float d_h = h * (float) tr[3];

            return new Rectangle2D.Float(d_x0, d_y0, d_w, d_h);
        }

        // It's an Affine

        //
        // Get sx0,sy0 coordinates and width & height of the source
        //
        float sx0 = (float) source.getMinX();
        float sy0 = (float) source.getMinY();
        float sw = (float) source.getWidth();
        float sh = (float) source.getHeight();

        //
        // The 4 points (clockwise order) are
        // (sx0, sy0), (sx0+sw, sy0)
        // (sx0, sy0+sh), (sx0+sw, sy0+sh)
        //
        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(sx0, sy0);
        pts[1] = new Point2D.Float((sx0 + sw), sy0);
        pts[2] = new Point2D.Float((sx0 + sw), (sy0 + sh));
        pts[3] = new Point2D.Float(sx0, (sy0 + sh));

        // Forward map
        forward_tr.transform(pts, 0, pts, 0, 4);

        float dx0 = Float.MAX_VALUE;
        float dy0 = Float.MAX_VALUE;
        float dx1 = -Float.MAX_VALUE;
        float dy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float) pts[i].getX();
            float py = (float) pts[i].getY();

            dx0 = Math.min(dx0, px);
            dy0 = Math.min(dy0, py);
            dx1 = Math.max(dx1, px);
            dy1 = Math.max(dy1, py);
        }

        //
        // Get the width & height of the resulting bounding box.
        // This is set on the layout
        //
        float lw = dx1 - dx0;
        float lh = dy1 - dy0;

        return new Rectangle2D.Float(dx0, dy0, lw, lh);
    }
}
