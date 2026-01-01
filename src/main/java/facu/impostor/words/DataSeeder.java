package facu.impostor.words;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final CategoryRepository categoryRepository;
    private final WordRepository wordRepository;

    public DataSeeder(CategoryRepository categoryRepository, WordRepository wordRepository) {
        this.categoryRepository = categoryRepository;
        this.wordRepository = wordRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0 && wordRepository.count() == 0) {
            ObjectMapper mapper = new ObjectMapper();
            SeedData seedData = mapper.readValue(new ClassPathResource("seed.json").getInputStream(), SeedData.class);
            // Guardar categorías primero y mapear por nombre
            for (Category c : seedData.getCategories()) {
                categoryRepository.save(c);
            }
            List<Category> allCategories = categoryRepository.findAll();
            for (Word w : seedData.getWords()) {
                // Buscar la categoría por nombre si viene embebida
                if (w.getCategory() != null && w.getCategory().getName() != null) {
                    String catName = w.getCategory().getName();
                    Category cat = allCategories.stream().filter(c -> c.getName().equals(catName)).findFirst().orElse(null);
                    w.setCategory(cat);
                }
                wordRepository.save(w);
            }
        }
    }

    public static class SeedData {
        private List<Category> categories;
        private List<Word> words;
        public List<Category> getCategories() { return categories; }
        public void setCategories(List<Category> categories) { this.categories = categories; }
        public List<Word> getWords() { return words; }
        public void setWords(List<Word> words) { this.words = words; }
    }
}
