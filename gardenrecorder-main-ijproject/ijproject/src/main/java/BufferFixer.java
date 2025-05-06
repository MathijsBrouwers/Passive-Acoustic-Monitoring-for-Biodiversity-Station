// class BufferFixer {
//     private final float[] buffer;
//     private int position = 0;

//     public BufferFixer(int targetSize) {
//         buffer = new float[targetSize];
//     }

//     public float[] addSamples(float[] samples) {
//         int copyLength = Math.min(samples.length, buffer.length - position);
//         System.arraycopy(samples, 0, buffer, position, copyLength);
//         position += copyLength;

//         if (position >= buffer.length) {
//             position = 0;
//             return buffer;
//         } else {
//             return null;
//         }
//     }
// }
