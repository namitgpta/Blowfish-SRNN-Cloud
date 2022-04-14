package com.namit.pw.blowfishsrnncloud.adapter;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namit.pw.blowfishsrnncloud.AwsCloudDataActivity;
import com.namit.pw.blowfishsrnncloud.AwsRdsData;
import com.namit.pw.blowfishsrnncloud.R;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AwsCloudDataRecyclerViewAdapter extends RecyclerView.Adapter<AwsCloudDataRecyclerViewAdapter.ViewHolder2> {

    private final Context context;
    private final ArrayList<String> timestampsArray, algorithmArray, filesExtensionArray;
    private final ArrayList<Integer> idArray;
    private final ArrayList<byte[]> encryptedFileBytesArray;
    boolean deleteEntrySuccessful, fileSavedOrNot;
    AlertDialog loadingDialog;

    private String yourKeyString;
    private byte[] decryptedFileBytes;

    String decryptionTimeTakenStr;
    long decryptionTimeLong;
    private String currentFileExtension;

    public AwsCloudDataRecyclerViewAdapter(Context context, ArrayList<String> timestampsArray, ArrayList<Integer> idArray, ArrayList<byte[]> encryptedFileBytesArray, ArrayList<String> algorithmArray, ArrayList<String> filesExtensionArray) {
        this.context = context;
        this.timestampsArray = timestampsArray;
        this.idArray = idArray;
        this.encryptedFileBytesArray = encryptedFileBytesArray;
        this.algorithmArray = algorithmArray;
        this.deleteEntrySuccessful = true;
        this.fileSavedOrNot = true;
        this.decryptionTimeTakenStr = "";
        this.filesExtensionArray = filesExtensionArray;

        // new Alert Loading Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setView(R.layout.loading_layout_dialog);
        loadingDialog = builder.create();
    }

    @NonNull
    @Override
    public AwsCloudDataRecyclerViewAdapter.ViewHolder2 onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.aws_cloud_data_row, viewGroup, false);
        return new AwsCloudDataRecyclerViewAdapter.ViewHolder2(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final AwsCloudDataRecyclerViewAdapter.ViewHolder2 holder, int position) {
        if (algorithmArray.isEmpty()) return;

        double sizeInKb = encryptedFileBytesArray.get(position).length / 1000.0;
        String dateTimeFullString = timestampsArray.get(position);
        String algo = algorithmArray.get(position);
        int id = idArray.get(position);

        holder.heading.setText(String.format("%s - %s File", algo, filesExtensionArray.get(position).toUpperCase()));

        DecimalFormat df = new DecimalFormat("0.00");
        String sizeStr = "";
        if (sizeInKb > 1000.0) {
            sizeStr += df.format(sizeInKb / 1000.0);
            sizeStr += " MB";
        } else {
            sizeStr += df.format(sizeInKb);
            sizeStr += " KB";
        }
        holder.sizeTextView.setText(sizeStr);

        String dateTimeStr = dateTimeFullString.substring(0, 11);
        int hrs = Integer.parseInt(dateTimeFullString.substring(11, 13));
        int mins = Integer.parseInt(dateTimeFullString.substring(14, 16));
        // to add +5:30 to the UTC timings stored in the table to convert to IST timings:
        hrs += 5;
        mins += 30;
        if (mins >= 60) {
            mins -= 60;
            hrs++;
        }
        if (hrs >= 24) {
            hrs -= 24;
        }
        if (hrs / 10 == 0) dateTimeStr += "0";
        dateTimeStr += String.valueOf(hrs);
        dateTimeStr += ":";
        if (mins / 10 == 0) dateTimeStr += "0";
        dateTimeStr += String.valueOf(mins);
        holder.date.setText(dateTimeStr);

        holder.saveBtn.setOnClickListener(v -> {
            currentFileExtension = filesExtensionArray.get(position);
            holder.timeTakenTextView.setText("");
            // alertDialog for key input:
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Decrypt the file & Save to your device:-");
            alert.setMessage("Enter the SRNN Key to decrypt your file (separated by spaces): ");
            // Set an EditText view to get user input
            EditText inputEditText = new EditText(context);
            alert.setView(inputEditText);
            alert.setPositiveButton("OK", (dialog, whichButton) -> {
                String srnnKeyStr = inputEditText.getText().toString().trim();
                String[] srnnKeysStrArray = srnnKeyStr.split(" ");
                decryptionTimeLong = System.currentTimeMillis();
                decodeSrnnKeys(srnnKeysStrArray);
                decryptionTimeLong = System.currentTimeMillis() - decryptionTimeLong;
                try {
                    decrypt(encryptedFileBytesArray.get(position), holder, algo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                // Canceled.
            });
            alert.show();
        });

        holder.deleteBtn.setOnClickListener(view -> {
            holder.deleteBtn.setVisibility(View.INVISIBLE);
            holder.progressBarDelete.setVisibility(View.VISIBLE);
            deleteEntryAwsRds(id, holder);
        });
    }

    @Override
    public int getItemCount() {
        return algorithmArray.size();
    }

    public static class ViewHolder2 extends RecyclerView.ViewHolder {
        public TextView date, sizeTextView, timeTakenTextView, heading;
        //        public ImageView imageView;
        public Button saveBtn, deleteBtn;
        public ProgressBar progressBarDelete;

        public ViewHolder2(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.textView_dateTime_awsCloudData_row);
            saveBtn = itemView.findViewById(R.id.saveBtn_awsCloudData_row);
            deleteBtn = itemView.findViewById(R.id.deleteBtn_awsCloudData_row);
            sizeTextView = itemView.findViewById(R.id.textView_size_awsCloudData_row);
            progressBarDelete = itemView.findViewById(R.id.progressBar_awsCloudData_row);
            timeTakenTextView = itemView.findViewById(R.id.timeTaken_textView_awsCloudData_row);
            heading = itemView.findViewById(R.id.textView_heading_awsCloudData_row);
//            imageView = itemView.findViewById(R.id.image_row_hf);

            progressBarDelete.setVisibility(View.INVISIBLE);
        }
    }

    private void deleteEntryAwsRds(int id, ViewHolder2 holder) {
        deleteEntrySuccessful = true;
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(AwsRdsData.url, AwsRdsData.username, AwsRdsData.password);

                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM Blowfish" + " WHERE _id=?");
                preparedStatement.setInt(1, id);
                preparedStatement.executeUpdate();
                connection.close();

            } catch (Exception e) {
                deleteEntrySuccessful = false;
                e.printStackTrace();
            }

            ((AwsCloudDataActivity) context).runOnUiThread(() -> {
                // after the job is finished:
                if (!deleteEntrySuccessful) {
                    Toast.makeText(context, "Error deleting the file!!!", Toast.LENGTH_LONG).show();
                } else {
                    String deletedStr = "deleted";
                    holder.deleteBtn.setText(deletedStr);
                    holder.deleteBtn.setTextColor(context.getColor(R.color.grey));
                    holder.deleteBtn.setClickable(false);
                }
                holder.progressBarDelete.setVisibility(View.INVISIBLE);
                holder.deleteBtn.setVisibility(View.VISIBLE);
            });
        }).start();

    }

    private void decodeSrnnKeys(String[] srnnKeysStrArray) {
        int num;
        BigInteger n, phi, u, a, p, q, v;
        int j;
        StringBuilder yourKeyStringBuilder = new StringBuilder();
        BigInteger t = new BigInteger("1");
        p = new BigInteger("11");
        q = new BigInteger("7");
        n = p.multiply(q);
        phi = (p.subtract(t)).multiply(q.subtract(t));
        u = new BigInteger("3");
        a = new BigInteger("31");

        BigInteger[] arr = new BigInteger[100];
        BigInteger[] dearr = new BigInteger[100];
        v = (u.pow(phi.intValue() - a.intValue())).mod(n);
        num = srnnKeysStrArray.length;
        for (j = 0; j < num; j++) {
            arr[j] = BigInteger.valueOf(Integer.parseInt(srnnKeysStrArray[j]));
        }
        for (j = 0; j < num; j++) {
            dearr[j] = (((v.pow(7)).multiply(arr[j])).pow(43)).mod(n);
            yourKeyStringBuilder.append((char) dearr[j].intValue());
        }
        Toast.makeText(context, "Your key is: " + yourKeyStringBuilder, Toast.LENGTH_SHORT).show();
        yourKeyString = yourKeyStringBuilder.toString();
    }

    public void decrypt(byte[] outputBytes, ViewHolder2 holder, String algo) {
        if (outputBytes == null) {
            System.out.println("outputBytes in aws adapter is null: line 185");
            return;
        }
        long startTime = System.currentTimeMillis();
        loadingDialog.show();
        new Thread(() -> {
            try {
                Key secretKey;
                Cipher cipher;
                String cipherAlgo = "Blowfish";
                if ("AES".equals(algo)) {
                    String salt_AES = "1234567890987654321";
                    secretKey = getKeyFromPassword_AES(yourKeyString, salt_AES);
                    cipherAlgo = algo;
                } else {
                    secretKey = new SecretKeySpec(yourKeyString.getBytes(), "Blowfish");
                }
                cipher = Cipher.getInstance(cipherAlgo);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

                decryptedFileBytes = cipher.doFinal(outputBytes);
                fileSavedOrNot = true;

            } catch (Exception e) {
                fileSavedOrNot = false;
                System.out.println("Error Exception raised aws recycler Line: " + Thread.currentThread().getStackTrace()[2].getLineNumber());
                e.printStackTrace();
            }

            ((AwsCloudDataActivity) context).runOnUiThread(() -> {
                if (fileSavedOrNot) {
                    if (decryptedFileBytes == null) {
                        Toast.makeText(context, "Some Internal Error while decrypting the file!!!. Check logs for error", Toast.LENGTH_LONG).show();
                    } else {
                        decryptionTimeTakenStr = "Decryption Time: ";
                        double timeTakenDouble = (System.currentTimeMillis() - startTime + decryptionTimeLong) / 1000.0;
                        decryptionTimeTakenStr += timeTakenDouble;
                        decryptionTimeTakenStr += " sec";
                        holder.timeTakenTextView.setText(decryptionTimeTakenStr);

                        generateFileAfterDecryption(algo);
                    }
                } else {
                    Toast.makeText(context, "Unable to Decrypt the file!!!. Check logs for error", Toast.LENGTH_LONG).show();
                }
                loadingDialog.dismiss();
            });
        }).start();
    }

    private void generateFileAfterDecryption(String algo) {
        loadingDialog.show();
        fileSavedOrNot = true;
        new Thread(() -> {
            OutputStream os = null;
            try {
                ContentResolver contentResolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                String currDateTime = LocalDateTime.now().toString();
//                System.out.println(currDateTime);
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Decrypted_File_" + algo + "_" + currDateTime.substring(0, currDateTime.length() - 7) + "." + currentFileExtension);
//                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/*");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "Blowfish_SRNN_Files");
                Uri keyUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                os = contentResolver.openOutputStream(Objects.requireNonNull(keyUri));
                os.write(decryptedFileBytes);
                Objects.requireNonNull(os);

                // Toasts not allowed in threads
            } catch (Exception e) {
                fileSavedOrNot = false;
                Log.e("Save Decrypted Bytes to file error in aws recycler: ", e.getMessage());
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ((AwsCloudDataActivity) context).runOnUiThread(() -> {
                // after the thread job is finished:
                loadingDialog.dismiss();
                if (fileSavedOrNot) {
                    Toast.makeText(context, "Decrypted File Saved to Downloads!!!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Unable to Save the decrypted bytes. Check logs for error", Toast.LENGTH_LONG).show();
                }
            });

        }).start();
    }

    private SecretKey getKeyFromPassword_AES(String password, String salt) {
        SecretKey secret = null;
        SecretKeyFactory factory;
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return secret;
    }
}
