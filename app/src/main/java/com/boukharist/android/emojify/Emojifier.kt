package com.boukharist.android.emojify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.Toast
import com.boukharist.android.emojify.Emojifier.Emoji.*
import com.example.android.emojify.R
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import timber.log.Timber


class Emojifier {
    companion object {

        private const val EMOJI_SCALE_FACTOR = .9f
        private const val SMILING_PROB_THRESHOLD = .15
        private const val EYE_OPEN_PROB_THRESHOLD = .5


        fun detectFacesAndOverlayEmoji(context: Context, backgroundPicture: Bitmap?): Bitmap? {
            val detector = FaceDetector.Builder(context)
                    .setTrackingEnabled(false)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                    .build()

            val frame = Frame.Builder().setBitmap(backgroundPicture).build()
            val faces = detector.detect(frame)

            // Log the number of faces
            Timber.d("detectFaces: number of faces = " + faces.size())

            var resultBitmap = backgroundPicture
            if (faces.size() == 0) {
                Toast.makeText(context, "No FACES DETECTED", Toast.LENGTH_LONG)
            } else {
                for (i in 0 until faces.size()) {
                    val face = faces.valueAt(i)
                    val emoji = whichEmoji(face)
                    val emojiBitmap = getEmojiBitmap(context, emoji)
                    resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face)
                }
            }

            detector.release()
            return resultBitmap
        }

        /**
         * Combines the original picture with the emoji bitmaps
         *
         * @param backgroundBitmap The original picture
         * @param emojiBitmap      The chosen emoji
         * @param face             The detected face
         * @return The final bitmap, including the emojis over the faces
         */
        private fun addBitmapToFace(backgroundBitmap: Bitmap?, emojiBitmap: Bitmap?, face: Face): Bitmap {
            backgroundBitmap!!
            emojiBitmap!!
            // Initialize the results bitmap to be a mutable copy of the original image
            val resultBitmap = Bitmap.createBitmap(backgroundBitmap.width,
                    backgroundBitmap.height, backgroundBitmap.config)

            // Scale the emoji so it looks better on the face
            val scaleFactor = EMOJI_SCALE_FACTOR

            // Determine the size of the emoji to match the width of the face and preserve aspect ratio
            val newEmojiWidth = (face.width * scaleFactor).toInt()
            val newEmojiHeight = (emojiBitmap.height * newEmojiWidth / emojiBitmap.width * scaleFactor).toInt()

            // Scale the emoji
            val emojiBitmapCopy = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false)

            // Determine the emoji position so it best lines up with the face
            val emojiPositionX = face.position.x + face.width / 2 - emojiBitmapCopy.width / 2
            val emojiPositionY = face.position.y + face.height / 2 - emojiBitmapCopy.height / 3

            // Create the canvas and draw the bitmaps to it
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(backgroundBitmap, 0F, 0F, null)
            canvas.drawBitmap(emojiBitmapCopy, emojiPositionX, emojiPositionY, null)

            return resultBitmap
        }

        private fun getEmojiBitmap(context: Context, emoji: Emoji): Bitmap {
            val drawableRes = when (emoji) {
                SMILE -> R.drawable.smile
                FROWN -> R.drawable.frown
                LEFT_WINK -> R.drawable.leftwink
                RIGHT_WINK -> R.drawable.rightwink
                LEFT_WINK_FROWN -> R.drawable.leftwinkfrown
                RIGHT_WINK_FROWN -> R.drawable.rightwinkfrown
                CLOSED_EYE_SMILE -> R.drawable.closed_smile
                CLOSED_EYE_FROWN -> R.drawable.closed_frown
            }

            return BitmapFactory.decodeResource(context.resources, drawableRes)
        }

        private fun whichEmoji(face: Face): Emoji {
            Timber.d("Smiling Prob : ${face.isSmilingProbability}")
            Timber.d("Left Eye Open Prob : ${face.isLeftEyeOpenProbability}")
            Timber.d("Right Eye Open Prob : ${face.isRightEyeOpenProbability}")

            val isSmiling = face.isSmilingProbability > SMILING_PROB_THRESHOLD
            val isLeftEyeOpen = face.isLeftEyeOpenProbability > EYE_OPEN_PROB_THRESHOLD
            val isRightEyeOpen = face.isRightEyeOpenProbability > EYE_OPEN_PROB_THRESHOLD

            val emoji = if (isSmiling) {
                when {
                    isLeftEyeOpen && isRightEyeOpen -> SMILE
                    isLeftEyeOpen && !isRightEyeOpen -> RIGHT_WINK
                    !isLeftEyeOpen && isRightEyeOpen -> LEFT_WINK
                    else -> CLOSED_EYE_SMILE
                }
            } else {
                when {
                    isLeftEyeOpen && isRightEyeOpen -> FROWN
                    isLeftEyeOpen && !isRightEyeOpen -> RIGHT_WINK_FROWN
                    !isLeftEyeOpen && isRightEyeOpen -> LEFT_WINK_FROWN
                    else -> CLOSED_EYE_FROWN
                }
            }

            Timber.d("EMOJI : $emoji")
            return emoji
        }
    }


    // Enum for all possible Emojis
    private enum class Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }
}