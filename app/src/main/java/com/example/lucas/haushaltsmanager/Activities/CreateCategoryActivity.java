package com.example.lucas.haushaltsmanager.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.lucas.haushaltsmanager.Database.Repositories.Categories.CategoryRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.Categories.Exceptions.CategoryNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildCategories.ChildCategoryRepository;
import com.example.lucas.haushaltsmanager.Dialogs.BasicTextInputDialog;
import com.example.lucas.haushaltsmanager.Dialogs.ColorPickerDialog;
import com.example.lucas.haushaltsmanager.Dialogs.SingleChoiceDialog;
import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.R;

import java.util.ArrayList;

public class CreateCategoryActivity extends AppCompatActivity {
    private static final String TAG = CreateCategoryActivity.class.getSimpleName();

    private Category mCategory;
    private ImageButton mBackArrow;
    private Button mCatNameBtn, mCatColorBtn, mSelectParentBtn, mCreateBtn;
    private RadioGroup mDefaultExpenseRadioGrp;
    private Category mParentCategory;

    private creationModes CREATION_MODE;

    private enum creationModes {
        UPDATE_CATEGORY,
        CREATE_CATEGORY
    }

    @Override
    protected void onCreate(Bundle savedInstances) {
        super.onCreate(savedInstances);
        setContentView(R.layout.activity_new_category);

        mCatNameBtn = findViewById(R.id.new_category_name);
        mCatColorBtn = findViewById(R.id.new_category_color);
        mSelectParentBtn = findViewById(R.id.new_category_select_parent);
        mCreateBtn = findViewById(R.id.new_category_create);
        mBackArrow = findViewById(R.id.back_arrow);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDefaultExpenseRadioGrp = findViewById(R.id.new_category_expense_type);

        resolveIntent(getIntent().getExtras());
    }

    @SuppressWarnings("ConstantConditions")
    private void resolveIntent(Bundle bundle) {
        if (bundle == null || !bundle.containsKey("mode"))
            throw new UnsupportedOperationException("Du musst den Modus setzten!");

        switch (bundle.getString("mode")) {
            case "updateCategory":

                CREATION_MODE = creationModes.UPDATE_CATEGORY;
                mCategory = bundle.getParcelable("updateCategory");

                mCreateBtn.setText(R.string.update);
                break;
            case "createCategory":

                CREATION_MODE = creationModes.CREATE_CATEGORY;
                mCategory = Category.createDummyCategory();

                mCreateBtn.setText(R.string.create_category);
                break;
            default:
                throw new UnsupportedOperationException("Modus " + bundle.getString("modus") + " wird nicht unterstützt!");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        mCatNameBtn.setHint(mCategory.getTitle());
        mCatNameBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Bundle args = new Bundle();
                args.putString("title", getResources().getString(R.string.category_name));

                BasicTextInputDialog basicDialog = new BasicTextInputDialog();
                basicDialog.setOnTextInputListener(new BasicTextInputDialog.OnTextInput() {

                    @Override
                    public void onTextInput(String textInput) {

                        mCategory.setName(textInput);
                        mCatNameBtn.setText(textInput);

                        Log.d(TAG, "set category name to: " + textInput);
                    }
                });
                basicDialog.setArguments(args);
                basicDialog.show(getFragmentManager(), "categoryName");
            }
        });

        mCatColorBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ColorPickerDialog colorPickerDialog = new ColorPickerDialog(CreateCategoryActivity.this, Color.WHITE);
                colorPickerDialog.setOnColorSelectedListener(new ColorPickerDialog.OnColorSelectedListener() {

                    @Override
                    public void onColorSelected(int color) {

                        mCategory.setColor(color);
                        Log.d(TAG, "set category color to: " + Integer.toHexString(color));
                    }
                });
                colorPickerDialog.show();
            }
        });

        mSelectParentBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(SingleChoiceDialog.TITLE, getString(R.string.choose_parent_category));
                bundle.putParcelableArrayList(SingleChoiceDialog.CONTENT, new ArrayList<Parcelable>(CategoryRepository.getAll()));
                bundle.putString(SingleChoiceDialog.ON_EMPTY_LIST_MESSAGE, "Clicke einfach auf auswählen um einen Neue Parent Kategorie zu erstellen");//todo übersetzung
                //todo wenn eine neue Parent Kategorie erstellt werden soll dann sollte ich den Parent einfach leer lassen
                //der parent bekommt dann einfach den gleichen namen wie die neu erstellte KindKategorie

                SingleChoiceDialog<Category> categoryPicker = new SingleChoiceDialog<>();
                categoryPicker.setArguments(bundle);
                categoryPicker.setOnEntrySelectedListener(new SingleChoiceDialog.OnEntrySelected() {
                    @Override
                    public void onEntrySelected(Object entry) {
                        Category parentCategory = (Category) entry;

                        if (parentCategory == null) {//todo kann man das vereinfachen

                            Bundle bundle1 = new Bundle();
                            bundle1.putString(BasicTextInputDialog.TITLE, getString(R.string.new_parent_category_name));

                            BasicTextInputDialog textInputDialog = new BasicTextInputDialog();
                            textInputDialog.setArguments(bundle1);
                            textInputDialog.setOnTextInputListener(new BasicTextInputDialog.OnTextInput() {

                                @Override
                                public void onTextInput(String textInput) {

                                    mParentCategory = CategoryRepository.insert(new Category(textInput, "#000000", false, new ArrayList<Category>()));
                                    mSelectParentBtn.setText(mParentCategory.getTitle());
                                }
                            });
                            textInputDialog.show(getFragmentManager(), "categoryParentName");

                        } else {

                            mParentCategory = parentCategory;
                            mSelectParentBtn.setText(mParentCategory.getTitle());
                        }
                    }
                });
                categoryPicker.show(getFragmentManager(), "create_category_parent");
            }
        });

        if (mCategory.getDefaultExpenseType())
            mDefaultExpenseRadioGrp.check(R.id.new_category_expense);
        else
            mDefaultExpenseRadioGrp.check(R.id.new_category_income);
        mDefaultExpenseRadioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.new_category_expense) {

                    mCategory.setDefaultExpenseType(true);
                } else {

                    mCategory.setDefaultExpenseType(false);
                }

                Log.d(TAG, "set expense type to : " + mCategory.getDefaultExpenseType());
            }
        });

        mCreateBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                switch (CREATION_MODE) {
                    case CREATE_CATEGORY:

                        if (mParentCategory == null) {

                            Toast.makeText(CreateCategoryActivity.this, "Wähle zuerst die übergeordnete Kategorie aus", Toast.LENGTH_SHORT).show();
                        } else {

                            ChildCategoryRepository.insert(mParentCategory, mCategory);
                            finish();
                        }
                        break;
                    case UPDATE_CATEGORY:

                        try {
                            CategoryRepository.update(mCategory);
                        } catch (CategoryNotFoundException e) {

                            Toast.makeText(CreateCategoryActivity.this, getString(R.string.category_not_found), Toast.LENGTH_SHORT).show();
                            //todo Fehlberbehandlung wenn versucht wird eine nicht existierende Kategorie zu updaten
                        }
                        finish();
                        break;
                }
            }
        });
    }
}
