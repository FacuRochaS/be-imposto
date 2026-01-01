package facu.impostor.words;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final WordRepository wordRepository;

    @Autowired
    public CategoryController(CategoryRepository categoryRepository, WordRepository wordRepository) {
        this.categoryRepository = categoryRepository;
        this.wordRepository = wordRepository;
    }

    @GetMapping
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @PostMapping
    public Category create(@RequestBody Category category) {
        return categoryRepository.save(category);
    }

    @PostMapping("/{id}/words")
    public Word addWordToCategory(@PathVariable Long id, @RequestBody Word word) {
        Category category = categoryRepository.findById(id).orElseThrow();
        word.setCategory(category);
        return wordRepository.save(word);
    }
}
